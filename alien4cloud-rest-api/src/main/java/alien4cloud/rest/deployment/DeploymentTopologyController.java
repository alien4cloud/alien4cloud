package alien4cloud.rest.deployment;

import static alien4cloud.utils.AlienUtils.safe;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.apache.commons.collections4.MapUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.component.repository.IFileRepository;
import alien4cloud.deployment.DeploymentTopologyService;
import alien4cloud.deployment.OrchestratorPropertiesValidationService;
import alien4cloud.deployment.model.DeploymentConfiguration;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.rest.application.model.SetLocationPoliciesRequest;
import alien4cloud.rest.application.model.UpdateDeploymentTopologyRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.topology.UpdatePropertyRequest;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintFunctionalException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.RestConstraintValidator;
import alien4cloud.utils.services.PropertyService;
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
    private DeploymentTopologyService deploymentTopologyService;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService appEnvironmentService;
    @Inject
    public IDeploymentTopologyHelper deploymentTopologyHelper;
    @Inject
    public PropertyService propertyService;
    @Inject
    private OrchestratorPropertiesValidationService orchestratorPropertiesValidationService;
    @Resource
    private IFileRepository artifactRepository;

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
        checkAuthorizations(appId, environmentId);
        DeploymentConfiguration deploymentConfiguration = deploymentTopologyService.getDeploymentConfiguration(environmentId);
        DeploymentTopologyDTO dto = deploymentTopologyHelper.buildDeploymentTopologyDTO(deploymentConfiguration);
        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(dto).build();
    }

    /**
     * Update application's input artifact.
     *
     * @param appId application Id
     * @param environmentId environment Id
     * @param inputArtifactId artifact's id
     * @return nothing if success, error will be handled in global exception strategy
     * @throws IOException
     */
    @ApiOperation(value = "Upload input artifact.", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/inputArtifacts/{inputArtifactId}/upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<DeploymentTopologyDTO> updateDeploymentInputArtifact(@PathVariable String appId, @PathVariable String environmentId,
            @PathVariable String inputArtifactId, @RequestParam("file") MultipartFile artifactFile) throws IOException {
        // Get the artifact to update
        checkAuthorizations(appId, environmentId);
        DeploymentTopology topology = deploymentTopologyService.getDeploymentTopology(environmentId);
        if (topology.getInputArtifacts() == null || !topology.getInputArtifacts().containsKey(inputArtifactId)) {
            throw new NotFoundException("Artifact with key [" + inputArtifactId + "] do not exist");
        }
        Map<String, DeploymentArtifact> artifacts = topology.getUploadedInputArtifacts();
        if (artifacts == null) {
            artifacts = new HashMap<>();
            topology.setUploadedInputArtifacts(artifacts);
        }
        DeploymentArtifact artifact = artifacts.get(inputArtifactId);
        if (artifact == null) {
            artifact = new DeploymentArtifact();
            artifacts.put(inputArtifactId, artifact);
        } else if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
            artifactRepository.deleteFile(artifact.getArtifactRef());
        }
        try (InputStream artifactStream = artifactFile.getInputStream()) {
            String artifactFileId = artifactRepository.storeFile(artifactStream);
            artifact.setArtifactName(artifactFile.getOriginalFilename());
            artifact.setArtifactRef(artifactFileId);
            artifact.setArtifactRepository(ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY);
            deploymentTopologyService.updateDeploymentTopologyInputsAndSave(topology);
            return RestResponseBuilder.<DeploymentTopologyDTO> builder()
                    .data(deploymentTopologyHelper.buildDeploymentTopologyDTO(deploymentTopologyService.getDeploymentConfiguration(environmentId))).build();
        }
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
        checkAuthorizations(appId, environmentId);
        DeploymentConfiguration deploymentConfiguration = deploymentTopologyService.updateSubstitution(environmentId, nodeId, locationResourceTemplateId);
        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(deploymentTopologyHelper.buildDeploymentTopologyDTO(deploymentConfiguration)).build();
    }

    @ApiOperation(value = "Update substitution's property.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/substitutions/{nodeId}/properties", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<?> updateSubstitutionProperty(@PathVariable String appId, @PathVariable String environmentId, @PathVariable String nodeId,
            @RequestBody UpdatePropertyRequest updateRequest) {
        checkAuthorizations(appId, environmentId);
        try {
            deploymentTopologyService.updateProperty(environmentId, nodeId, updateRequest.getPropertyName(), updateRequest.getPropertyValue());
            return RestResponseBuilder.<DeploymentTopologyDTO> builder()
                    .data(deploymentTopologyHelper.buildDeploymentTopologyDTO(deploymentTopologyService.getDeploymentConfiguration(environmentId))).build();
        } catch (ConstraintFunctionalException e) {
            return RestConstraintValidator.fromException(e, updateRequest.getPropertyName(), updateRequest.getPropertyValue());
        }
    }

    @ApiOperation(value = "Update substitution's capability property.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/substitutions/{nodeId}/capabilities/{capabilityName}/properties", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<?> updateSubstitutionCapabilityProperty(@PathVariable String appId, @PathVariable String environmentId, @PathVariable String nodeId,
            @PathVariable String capabilityName, @RequestBody UpdatePropertyRequest updateRequest) {
        checkAuthorizations(appId, environmentId);
        try {
            deploymentTopologyService.updateCapabilityProperty(environmentId, nodeId, capabilityName, updateRequest.getPropertyName(),
                    updateRequest.getPropertyValue());
            return RestResponseBuilder.<DeploymentTopologyDTO> builder()
                    .data(deploymentTopologyHelper.buildDeploymentTopologyDTO(deploymentTopologyService.getDeploymentConfiguration(environmentId))).build();
        } catch (ConstraintFunctionalException e) {
            return RestConstraintValidator.fromException(e, updateRequest.getPropertyName(), updateRequest.getPropertyValue());
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
        checkAuthorizations(appId, environmentId);
        DeploymentConfiguration deploymentConfiguration = deploymentTopologyService.setLocationPolicies(environmentId, request.getOrchestratorId(),
                request.getGroupsToLocations());
        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(deploymentTopologyHelper.buildDeploymentTopologyDTO(deploymentConfiguration)).build();
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
        // check rights on related environment
        checkAuthorizations(appId, environmentId);

        DeploymentConfiguration deploymentConfiguration = deploymentTopologyService.getDeploymentConfiguration(environmentId);
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();

        try {
            ToscaContext.init(deploymentTopology.getDependencies());
            // update topology inputs
            for (Map.Entry<String, Object> inputPropertyValue : safe(updateRequest.getInputProperties()).entrySet()) {
                if (deploymentTopology.getInputs() == null || deploymentTopology.getInputs().get(inputPropertyValue.getKey()) == null) {
                    throw new NotFoundException("Input", inputPropertyValue.getKey(), "Input <" + inputPropertyValue.getKey()
                            + "> cannot be found on topology for application <" + appId + "> environement <" + environmentId + ">");
                }
                propertyService.setPropertyValue(deploymentTopology.getInputProperties(), deploymentTopology.getInputs().get(inputPropertyValue.getKey()),
                        inputPropertyValue.getKey(), inputPropertyValue.getValue());
            }

            // update
            if (MapUtils.isNotEmpty(updateRequest.getProviderDeploymentProperties())) {
                deploymentTopology.getProviderDeploymentProperties().putAll(updateRequest.getProviderDeploymentProperties());
                orchestratorPropertiesValidationService.checkConstraints(deploymentTopology.getOrchestratorId(),
                        updateRequest.getProviderDeploymentProperties());
            }

            deploymentTopologyService.updateDeploymentTopologyInputsAndSave(deploymentTopology);
        } catch (ConstraintViolationException e) {
            return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
            return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        } finally {
            ToscaContext.destroy();
        }

        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(deploymentTopologyHelper.buildDeploymentTopologyDTO(deploymentConfiguration)).build();
    }

    /**
     * Security check on application and environment
     *
     * @param appId application's id
     * @param environmentId environment's id
     */
    private void checkAuthorizations(String appId, String environmentId) {
        Application application = applicationService.getOrFail(appId);
        ApplicationEnvironment environment = appEnvironmentService.getOrFail(environmentId);
        // // Security check user must be authorized to deploy the environment (or be application manager)
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
    }

}
