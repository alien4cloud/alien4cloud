package alien4cloud.rest.deployment;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.Authorization;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.AlienConstants;
import alien4cloud.deployment.DeploymentNodeSubstitutionService;
import alien4cloud.deployment.DeploymentTopologyService;
import alien4cloud.deployment.DeploymentTopologyValidationService;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.DeploymentConfiguration;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceService;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.rest.application.model.SetLocationPoliciesRequest;
import alien4cloud.rest.application.model.UpdateDeploymentTopologyRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyService;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.ReflectionUtil;

@RestController
@RequestMapping("/rest/applications/{appId}/environments/{environmentId}/deployment-topology")
@Api(value = "", description = "Manage configuration of an application before deploying it.")
public class DeploymentTopologyController {

    @Inject
    private DeploymentTopologyService deploymentTopologyService;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService appEnvironmentService;
    @Inject
    private DeploymentNodeSubstitutionService deploymentNodeSubstitutionService;
    @Inject
    private LocationResourceService locationResourceService;
    @Inject
    private TopologyService topologyService;
    @Inject
    private DeploymentTopologyValidationService deploymentTopologyValidationService;

    /**
     * Update node substitution
     *
     * @param appId         id of the application
     * @param environmentId id of the environment
     * @return response containing the available substitutions
     */
    @ApiOperation(value = "Substitute a specific node by the location resource template in the topology of an application given an environment.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/substitutions/{nodeId}", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<DeploymentTopologyDTO> updateSubstitution(@PathVariable String appId, @PathVariable String environmentId, @PathVariable String nodeId,
                                                                  @RequestParam String locationResourceTemplateId) {
        checkAuthorizations(appId, environmentId);
        DeploymentConfiguration deploymentConfiguration = deploymentTopologyService.getDeploymentConfiguration(environmentId);
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();
        locationResourceService.getOrFail(locationResourceTemplateId);
        deploymentTopology.getSubstitutedNodes().put(nodeId, locationResourceTemplateId);
        deploymentTopologyService.updateDeploymentTopology(deploymentTopology);
        return RestResponseBuilder.<DeploymentTopologyDTO>builder().data(buildDeploymentTopologyDTO(deploymentConfiguration)).build();
    }

    @ApiOperation(value = "Update substitution's property.", authorizations = {@Authorization("ADMIN")})
    @RequestMapping(value = "/substitutions/{nodeId}/properties", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<DeploymentTopologyDTO> updateSubstitutionProperty(@PathVariable String appId, @PathVariable String environmentId,
                                                                          @PathVariable String nodeId, @RequestBody UpdateSubstitutionPropertyRequest updateRequest) {
        DeploymentConfiguration deploymentConfiguration = deploymentTopologyService.getDeploymentConfiguration(environmentId);
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();
        deploymentTopologyService.updateSubstitutionProperty(deploymentTopology, nodeId, updateRequest.getPropertyName(), updateRequest.getPropertyValue());
        return RestResponseBuilder.<DeploymentTopologyDTO>builder().data(buildDeploymentTopologyDTO(deploymentConfiguration)).build();
    }

    @ApiOperation(value = "Update substitution's capability property.", authorizations = {@Authorization("ADMIN")})
    @RequestMapping(value = "/substitutions/{nodeId}/capabilities/{capabilityName}/properties", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<DeploymentTopologyDTO> updateSubstitutionCapabilityProperty(@PathVariable String appId, @PathVariable String environmentId,
                                                                                    @PathVariable String nodeId, @PathVariable String capabilityName, @RequestBody UpdateSubstitutionPropertyRequest updateRequest) {
        DeploymentConfiguration deploymentConfiguration = deploymentTopologyService.getDeploymentConfiguration(environmentId);
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();
        deploymentTopologyService.updateSubstitutionCapabilityProperty(deploymentTopology, nodeId, capabilityName, updateRequest.getPropertyName(),
                updateRequest.getPropertyValue());
        return RestResponseBuilder.<DeploymentTopologyDTO>builder().data(buildDeploymentTopologyDTO(deploymentConfiguration)).build();
    }

    /**
     * Get the deployment topology of an application given an environment
     *
     * @param appId         application Id
     * @param environmentId environment Id
     * @return the deployment topology DTO
     */
    @ApiOperation(value = "Get the deployment topology of an application given an environment.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<DeploymentTopologyDTO> getDeploymentTopology(@PathVariable String appId, @PathVariable String environmentId) {
        RestResponseBuilder<DeploymentTopologyDTO> responseBuilder = RestResponseBuilder.<DeploymentTopologyDTO>builder();
        checkAuthorizations(appId, environmentId);
        DeploymentConfiguration deploymentConfiguration = deploymentTopologyService.getDeploymentConfiguration(environmentId);
        DeploymentTopologyDTO dto = buildDeploymentTopologyDTO(deploymentConfiguration);
        return responseBuilder.data(dto).build();
    }

    /**
     * Set location policies for a deployment. Creates if not yet the {@link DeploymentTopology} object linked to this deployment
     *
     * @param appId   application Id
     * @param request {@link SetLocationPoliciesRequest} object: location policies
     * @return
     */
    @ApiOperation(value = "Set location policies for a deployment. Creates if not yet the {@link DeploymentTopology} object linked to this deployment.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/location-policies", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    @PreAuthorize("isAuthenticated()")
    public RestResponse<DeploymentTopologyDTO> setLocationPolicies(@PathVariable String appId, @PathVariable String environmentId,
                                                                   @RequestBody SetLocationPoliciesRequest request) {
        RestResponseBuilder<DeploymentTopologyDTO> responseBuilder = RestResponseBuilder.builder();

        checkAuthorizations(appId, environmentId);
        DeploymentConfiguration deploymentConfiguration = deploymentTopologyService.setLocationPolicies(environmentId, request.getOrchestratorId(),
                request.getGroupsToLocations());
        return responseBuilder.data(buildDeploymentTopologyDTO(deploymentConfiguration)).build();
    }

    // FIXME THIS CANNOT BE USED ANYMORE TO SET MATCHING RESULT

    /**
     * Update application's deployment setup
     *
     * @param appId The application id.
     * @return nothing if success, error will be handled in global exception strategy
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
        ReflectionUtil.mergeObject(updateRequest, deploymentTopology);
        if (deploymentTopology.getInputProperties() != null) {
            // If someone modified the input properties, must validate them
            try {
                deploymentTopologyValidationService.validateInputProperties(deploymentTopology);
            } catch (ConstraintViolationException e) {
                return RestResponseBuilder.<ConstraintUtil.ConstraintInformation>builder().data(e.getConstraintInformation())
                        .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                return RestResponseBuilder.<ConstraintUtil.ConstraintInformation>builder().data(e.getConstraintInformation())
                        .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
            }
        }
        deploymentTopologyService.updateDeploymentTopologyInputsAndSave(deploymentTopology);
        return RestResponseBuilder.<DeploymentTopologyDTO>builder().data(buildDeploymentTopologyDTO(deploymentConfiguration)).build();
    }

    /**
     * Security check on application and environment
     *
     * @param appId         application's id
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

    private DeploymentTopologyDTO buildDeploymentTopologyDTO(DeploymentConfiguration deploymentConfiguration) {
        DeploymentTopology deploymentTopology = deploymentConfiguration.getDeploymentTopology();
        TopologyDTO topologyDTO = topologyService.buildTopologyDTO(deploymentTopology);
        DeploymentTopologyDTO deploymentTopologyDTO = new DeploymentTopologyDTO();
        ReflectionUtil.mergeObject(topologyDTO, deploymentTopologyDTO);
        String locationId = TopologyLocationUtils.getLocationId(deploymentTopology);
        if (StringUtils.isNotBlank(locationId)) {
            deploymentTopologyDTO.getLocationPolicies().put(AlienConstants.GROUP_ALL, locationId);
        }
        deploymentTopologyDTO.setAvailableSubstitutions(deploymentConfiguration.getAvailableSubstitutions());
        deploymentTopologyDTO.setValidation(deploymentTopologyValidationService.validateDeploymentTopology(deploymentTopology));
        Map<String, LocationResourceTemplate> templates = locationResourceService.getMultiple(deploymentTopology.getSubstitutedNodes().values());
        deploymentTopologyDTO.setLocationResourceTemplates(templates);
        return deploymentTopologyDTO;
    }
}
