package alien4cloud.rest.deployment;

import java.io.InputStream;
import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.alien4cloud.alm.deployment.configuration.services.InputArtifactService;
import org.alien4cloud.alm.deployment.configuration.services.InputService;
import org.alien4cloud.alm.deployment.configuration.services.LocationMatchService;
import org.alien4cloud.alm.deployment.configuration.services.MatchedNodePropertiesConfigService;
import org.alien4cloud.alm.deployment.configuration.services.MatchedPolicyPropertiesConfigService;
import org.alien4cloud.alm.deployment.configuration.services.NodeMatchingSubstitutionService;
import org.alien4cloud.alm.deployment.configuration.services.OrchestratorPropertiesService;
import org.alien4cloud.alm.deployment.configuration.services.PolicyMatchingSubstitutionService;
import org.alien4cloud.tosca.exceptions.ConstraintFunctionalException;
import org.alien4cloud.tosca.exceptions.ConstraintTechnicalException;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.deployment.DeploymentTopologyDTO;
import alien4cloud.deployment.DeploymentTopologyDTOBuilder;
import alien4cloud.deployment.IDeploymentConfigAction;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationTopologyVersion;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.rest.application.model.SetLocationPoliciesRequest;
import alien4cloud.rest.application.model.UpdateDeploymentTopologyRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.topology.UpdatePropertyRequest;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.RestConstraintValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

@RestController
@RequestMapping({ "/rest/applications/{appId}/environments/{environmentId}/deployment-topology",
        "/rest/v1/applications/{appId}/environments/{environmentId}/deployment-topology",
        "/rest/latest/applications/{appId}/environments/{environmentId}/deployment-topology" })
@Api(value = "", description = "Prepare a topology to be deployed on a specific environment (location matching, node matching and inputs configuration).")
public class DeploymentTopologyController {
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService appEnvironmentService;
    @Inject
    private ApplicationVersionService applicationVersionService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private DeploymentTopologyDTOBuilder deploymentTopologyDTOBuilder;
    @Inject
    private InputArtifactService inputArtifactService;
    @Inject
    private NodeMatchingSubstitutionService nodeMatchingSubstitutionService;
    @Inject
    private PolicyMatchingSubstitutionService policyMatchingSubstitutionService;
    @Inject
    private MatchedNodePropertiesConfigService matchedNodePropertiesConfigService;
    @Inject
    private MatchedPolicyPropertiesConfigService matchedPolicyPropertiesConfigService;
    @Inject
    private LocationMatchService locationMatchService;
    @Inject
    private InputService inputService;
    @Inject
    private OrchestratorPropertiesService orchestratorPropertiesService;

    /**
     * Get the deployment topology of an application given an environment
     *
     * @param appId application Id
     * @param environmentId environment Id
     * @return the deployment topology DTO
     */
    @ApiOperation(value = "Get the deployment topology of an application given an environment.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<DeploymentTopologyDTO> getDeploymentTopology(@PathVariable String appId, @PathVariable String environmentId) {
        Application application = applicationService.getOrFail(appId);
        ApplicationEnvironment environment = appEnvironmentService.getOrFail(environmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);

        ApplicationTopologyVersion topologyVersion = applicationVersionService
                .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
        Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());

        // This method prepares the deployment and create a Deployment Topology DTO object.
        DeploymentTopologyDTO dto = deploymentTopologyDTOBuilder.prepareDeployment(topology, application, environment);

        if (dto.getAvailableSubstitutions() != null && dto.getAvailableSubstitutions().getSubstitutionTypes() != null) {
            // Fix for Services with abstract types
            //  When the service is abstract, the type goes in configurationTypes but
            // the UI need it also in nodeTypes
            dto.getAvailableSubstitutions().getSubstitutionTypes().getNodeTypes().putAll(
                    dto.getAvailableSubstitutions().getSubstitutionTypes().getConfigurationTypes()
            );
        }
        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
    }

    /**
     * Upload an artifact to set it as input artifact.
     *
     * @param appId application Id
     * @param environmentId environment Id
     * @param inputArtifactId input artifact's id
     * @param artifactFile The multipart file that contains the uploaded artifact.
     * @return The deployment topology dto that contains the current validation state for a deployment.
     */
    @ApiOperation(value = "Upload input artifact.", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/inputArtifacts/{inputArtifactId}/upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<DeploymentTopologyDTO> uploadDeploymentInputArtifact (@PathVariable String appId, @PathVariable String environmentId, 
                                                                              @PathVariable String inputArtifactId, HttpServletRequest request) {
        DeploymentTopologyDTO dto = null;
        try {
            ServletFileUpload upload = new ServletFileUpload();
            FileItemIterator iter = upload.getItemIterator(request);
            if (iter.hasNext()) {
               FileItemStream item = iter.next();
               InputStream stream = item.openStream();
               if (!item.isFormField()) {
                  String fileName = item.getName();
                  dto = execute(appId, environmentId, (application, environment, topologyVersion, topology) -> {
                     try {
                        inputArtifactService.updateInputArtifact(environment, topology, inputArtifactId, stream, fileName);
                     } catch (IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                     }
                  });
               }
               stream.close();
            }
        } catch (IOException | FileUploadException e) {
            throw new RuntimeException(e.getMessage(), e);
        }        

        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
    }

    /**
     * Configure an input artifact to be fetched from repositories.
     *
     * @param appId application Id
     * @param environmentId environment Id
     * @param inputArtifactId input artifact's id
     * @param artifact The definition of the artifact to be fetched from repository.
     * @return The deployment topology dto that contains the current validation state for a deployment.
     */
    @RequestMapping(value = "/inputArtifacts/{inputArtifactId}/update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<DeploymentTopologyDTO> updateDeploymentInputArtifact(@PathVariable String appId, @PathVariable String environmentId,
            @PathVariable String inputArtifactId, @RequestBody DeploymentArtifact artifact) {
        DeploymentTopologyDTO dto = execute(appId, environmentId, (application, environment, topologyVersion, topology) -> {
            inputArtifactService.updateInputArtifact(environment, topology, inputArtifactId, artifact);
        });

        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
    }

    /**
     * Update node substitution.
     *
     * @param appId id of the application.
     * @param environmentId id of the environment.
     * @return response containing the available substitutions.
     */
    @ApiOperation(value = "Substitute a specific node by the location resource template in the topology of an application given an environment.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/substitutions/{nodeId}", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<DeploymentTopologyDTO> updateSubstitution(@PathVariable String appId, @PathVariable String environmentId, @PathVariable String nodeId,
            @RequestParam String locationResourceTemplateId) {
        Application application = applicationService.getOrFail(appId);
        ApplicationEnvironment environment = appEnvironmentService.getOrFail(environmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);

        ApplicationTopologyVersion topologyVersion = applicationVersionService
                .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
        Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());

        DeploymentTopologyDTO dto = deploymentTopologyDTOBuilder.prepareDeployment(topology,
                () -> nodeMatchingSubstitutionService.updateSubstitution(application, environment, topology, nodeId, locationResourceTemplateId));

        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
    }

    /**
     * Update policy substitution.
     *
     * @param appId id of the application.
     * @param environmentId id of the environment.
     * @return response containing the deployment topology dto {@link DeploymentTopologyDTO}.
     */
    @ApiOperation(value = "Substitute a specific policy by a location policy resource template in the topology of an application, given an environment.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/policies/{policyId}/substitution", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<DeploymentTopologyDTO> updatePolicySubstitution(@PathVariable String appId, @PathVariable String environmentId,
            @PathVariable String policyId, @RequestParam String locationResourceTemplateId) {
        Application application = applicationService.getOrFail(appId);
        ApplicationEnvironment environment = appEnvironmentService.getOrFail(environmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);

        ApplicationTopologyVersion topologyVersion = applicationVersionService
                .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
        Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());

        DeploymentTopologyDTO dto = deploymentTopologyDTOBuilder.prepareDeployment(topology,
                () -> policyMatchingSubstitutionService.updateSubstitution(application, environment, topology, policyId, locationResourceTemplateId));

        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
    }

    @ApiOperation(value = "Update node substitution's property.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/substitutions/{nodeId}/properties", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<?> updateSubstitutionProperty(@PathVariable String appId, @PathVariable String environmentId, @PathVariable String nodeId,
            @RequestBody UpdatePropertyRequest updateRequest) {
        try {
            Application application = applicationService.getOrFail(appId);
            ApplicationEnvironment environment = appEnvironmentService.getOrFail(environmentId);
            AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);

            ApplicationTopologyVersion topologyVersion = applicationVersionService
                    .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
            Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());

            DeploymentTopologyDTO dto = deploymentTopologyDTOBuilder.prepareDeployment(topology,
                    () -> matchedNodePropertiesConfigService.updateProperty(application, environment, topology, nodeId, Optional.empty(),
                            updateRequest.getPropertyName(), updateRequest.getPropertyValue()));

            return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
        } catch (ConstraintTechnicalException e) {
            if (e.getCause() instanceof ConstraintFunctionalException) {
                return RestConstraintValidator.fromException((ConstraintFunctionalException) e.getCause(), updateRequest.getPropertyName(),
                        updateRequest.getPropertyValue());
            }
            throw e;
        }
    }

    @ApiOperation(value = "Update policy substitution's property.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/policies/{nodeId}/substitution/properties", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<?> updatePolicySubstitutionProperty(@PathVariable String appId, @PathVariable String environmentId, @PathVariable String nodeId,
            @RequestBody UpdatePropertyRequest updateRequest) {
        try {
            Application application = applicationService.getOrFail(appId);
            ApplicationEnvironment environment = appEnvironmentService.getOrFail(environmentId);
            AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);

            ApplicationTopologyVersion topologyVersion = applicationVersionService
                    .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
            Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());

            DeploymentTopologyDTO dto = deploymentTopologyDTOBuilder.prepareDeployment(topology, () -> matchedPolicyPropertiesConfigService
                    .updateProperty(application, environment, topology, nodeId, updateRequest.getPropertyName(), updateRequest.getPropertyValue()));

            return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
        } catch (ConstraintTechnicalException e) {
            if (e.getCause() instanceof ConstraintFunctionalException) {
                return RestConstraintValidator.fromException((ConstraintFunctionalException) e.getCause(), updateRequest.getPropertyName(),
                        updateRequest.getPropertyValue());
            }
            throw e;
        }
    }

    @ApiOperation(value = "Update substitution's capability property.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/substitutions/{nodeId}/capabilities/{capabilityName}/properties", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<?> updateSubstitutionCapabilityProperty(@PathVariable String appId, @PathVariable String environmentId, @PathVariable String nodeId,
            @PathVariable String capabilityName, @RequestBody UpdatePropertyRequest updateRequest) {

        try {
            Application application = applicationService.getOrFail(appId);
            ApplicationEnvironment environment = appEnvironmentService.getOrFail(environmentId);
            AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);

            ApplicationTopologyVersion topologyVersion = applicationVersionService
                    .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
            Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());

            DeploymentTopologyDTO dto = deploymentTopologyDTOBuilder.prepareDeployment(topology,
                    () -> matchedNodePropertiesConfigService.updateProperty(application, environment, topology, nodeId, Optional.of(capabilityName),
                            updateRequest.getPropertyName(), updateRequest.getPropertyValue()));

            return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
        } catch (ConstraintTechnicalException e) {
            if (e.getCause() instanceof ConstraintFunctionalException) {
                return RestConstraintValidator.fromException((ConstraintFunctionalException) e.getCause(), updateRequest.getPropertyName(),
                        updateRequest.getPropertyValue());
            }
            throw e;
        }
    }

    /**
     * Set location policies for a deployment. Creates if not yet the {@link DeploymentTopology} object linked to this deployment
     *
     * @param appId application Id
     * @param request {@link SetLocationPoliciesRequest} object: location policies
     * @return
     */
    @ApiOperation(value = "Set location policies for a deployment. Creates if not yet the {@link DeploymentTopology} object linked to this deployment.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/location-policies", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    @PreAuthorize("isAuthenticated()")
    public RestResponse<DeploymentTopologyDTO> setLocationPolicies(@ApiParam(value = "Id of the application.", required = true) @PathVariable String appId,
            @ApiParam(value = "Id of the environment on which to set the location policies.", required = true) @PathVariable String environmentId,
            @ApiParam(value = "Location policies request body.", required = true) @RequestBody SetLocationPoliciesRequest request) {
        DeploymentTopologyDTO dto = execute(appId, environmentId, (application, environment, topologyVersion, topology) -> {
            locationMatchService.setLocationPolicy(environment, request.getOrchestratorId(), request.getGroupsToLocations());
        });

        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
    }

    /**
     * 
     * @param appId The application id
     * @param environmentId Id of the environment we want to update
     * @param updateRequest an {@link UpdateDeploymentTopologyRequest} object
     * @return a {@link RestResponse} with:<br>
     *         the {@link DeploymentTopologyDTO} if everything went well, the <br>
     *         Error if not
     *
     * @throws OrchestratorDisabledException
     */
    @ApiOperation(value = "Updates by merging the given request into the given application's deployment topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<?> updateDeploymentSetup(@PathVariable String appId, @PathVariable String environmentId,
            @RequestBody UpdateDeploymentTopologyRequest updateRequest) throws OrchestratorDisabledException {
        try {
            // check rights on related environment
            DeploymentTopologyDTO dto = execute(appId, environmentId, (application, environment, topologyVersion, topology) -> {
                // Set inputs
                inputService.setInputValues(environment, topology, updateRequest.getInputProperties());
                // Set orchestrator specific properties
                orchestratorPropertiesService.setOrchestratorProperties(environment, updateRequest.getProviderDeploymentProperties());
            });

            return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
        } catch (ConstraintTechnicalException e) {
            if (e.getCause() instanceof ConstraintFunctionalException) {
                ConstraintFunctionalException ex = (ConstraintFunctionalException) e.getCause();
                return RestConstraintValidator.fromException(ex, ex.getConstraintInformation().getName(), ex.getConstraintInformation().getValue());
            }
            throw e;
        }
    }

    private DeploymentTopologyDTO execute(String applicationId, String environmentId, IDeploymentConfigAction action) {
        Application application = applicationService.getOrFail(applicationId);
        ApplicationEnvironment environment = appEnvironmentService.getOrFail(environmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);

        ApplicationTopologyVersion topologyVersion = applicationVersionService
                .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
        Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());

        // This method prepares the deployment and create a Deployment Topology DTO object.
        return deploymentTopologyDTOBuilder.prepareDeployment(topology, application, environment, topologyVersion, action);
    }
}