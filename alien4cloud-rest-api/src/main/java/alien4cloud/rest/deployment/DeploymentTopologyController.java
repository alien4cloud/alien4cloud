package alien4cloud.rest.deployment;

import java.util.List;
import java.util.Set;

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

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.AlienConstants;
import alien4cloud.deployment.DeploymentNodeSubstitutionService;
import alien4cloud.deployment.DeploymentTopologyService;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceService;
import alien4cloud.rest.application.model.SetLocationPoliciesRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyService;
import alien4cloud.utils.ReflectionUtil;

import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.Authorization;

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

    /**
     * Try to get the available substitutions for node of the topology of the given application on the given environment
     * 
     * @param appId id of the application
     * @param environmentId id of the environment
     * @return response containing the available substitutions
     */
    @ApiOperation(value = "Get available substitutions for the topology of an application given an environment.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/substitutions", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<DeploymentNodeSubstitutionsDTO> getAvailableNodeSubstitutions(@PathVariable String appId, @PathVariable String environmentId) {
        checkAuthorizations(appId, environmentId);
        DeploymentTopology deploymentTopology = deploymentTopologyService.getOrCreateDeploymentTopology(environmentId);
        DeploymentNodeSubstitutionsDTO dto = new DeploymentNodeSubstitutionsDTO();
        dto.setAvailableSubstitutions(deploymentNodeSubstitutionService.getAvailableSubstitutions(deploymentTopology));
        Set<LocationResourceTemplate> allTemplates = Sets.newHashSet();
        for (List<LocationResourceTemplate> availableSubstitutions : dto.getAvailableSubstitutions().values()) {
            allTemplates.addAll(availableSubstitutions);
        }
        dto.setSubstitutionTypes(locationResourceService.getLocationResourceTypes(allTemplates));
        return RestResponseBuilder.<DeploymentNodeSubstitutionsDTO> builder().data(dto).build();
    }

    /**
     * Update node substitution
     *
     * @param appId id of the application
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
        DeploymentTopology deploymentTopology = deploymentTopologyService.getOrCreateDeploymentTopology(environmentId);
        LocationResourceTemplate locationResourceTemplate = locationResourceService.getOrFail(locationResourceTemplateId);
        deploymentTopology.getSubstitutedNodes().put(nodeId, locationResourceTemplate);
        deploymentTopologyService.updateDeploymentTopology(deploymentTopology);
        return RestResponseBuilder.<DeploymentTopologyDTO> builder().data(buildDeploymentTopologyDTO(deploymentTopology)).build();
    }

    @ApiOperation(value = "Update substitution's property.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/substitutions/{nodeId}/properties", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> updateSubstitutionProperty(@PathVariable String appId, @PathVariable String environmentId, @PathVariable String nodeId,
            @RequestBody UpdateSubstitutionPropertyRequest updateRequest) {
        deploymentTopologyService.updateSubstitutionProperty(deploymentTopologyService.getOrCreateDeploymentTopology(environmentId), nodeId,
                updateRequest.getPropertyName(), updateRequest.getPropertyValue());
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Update substitution's capability property.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/substitutions/{nodeId}/capabilities/{capabilityName}/properties", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> updateSubstitutionCapabilityProperty(@PathVariable String appId, @PathVariable String environmentId, @PathVariable String nodeId,
            @PathVariable String capabilityName, @RequestBody UpdateSubstitutionPropertyRequest updateRequest) {
        deploymentTopologyService.updateSubstitutionCapabilityProperty(deploymentTopologyService.getOrCreateDeploymentTopology(environmentId), nodeId,
                capabilityName, updateRequest.getPropertyName(), updateRequest.getPropertyValue());
        return RestResponseBuilder.<Void> builder().build();
    }

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
        RestResponseBuilder<DeploymentTopologyDTO> responseBuilder = RestResponseBuilder.<DeploymentTopologyDTO> builder();
        checkAuthorizations(appId, environmentId);
        DeploymentTopology deploymentTopology = deploymentTopologyService.getOrCreateDeploymentTopology(environmentId);
        DeploymentTopologyDTO dto = buildDeploymentTopologyDTO(deploymentTopology);
        return responseBuilder.data(dto).build();
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
    public RestResponse<DeploymentTopologyDTO> setLocationPolicies(@PathVariable String appId, @PathVariable String environmentId,
            @RequestBody SetLocationPoliciesRequest request) {
        RestResponseBuilder<DeploymentTopologyDTO> responseBuilder = RestResponseBuilder.builder();

        checkAuthorizations(appId, environmentId);
        DeploymentTopology deploymentTopology = deploymentTopologyService.setLocationPolicies(environmentId, request.getOrchestratorId(),
                request.getGroupsToLocations());
        return responseBuilder.data(buildDeploymentTopologyDTO(deploymentTopology)).build();
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

    private DeploymentTopologyDTO buildDeploymentTopologyDTO(DeploymentTopology deploymentTopology) {
        TopologyDTO topologyDTO = topologyService.buildTopologyDTO(deploymentTopology);
        DeploymentTopologyDTO deploymentTopologyDTO = new DeploymentTopologyDTO();
        ReflectionUtil.mergeObject(topologyDTO, deploymentTopologyDTO);
        String locationId = TopologyLocationUtils.getLocationId(deploymentTopology);
        if (StringUtils.isNotBlank(locationId)) {
            deploymentTopologyDTO.getLocationPolicies().put(AlienConstants.GROUP_ALL, locationId);
        }
        return deploymentTopologyDTO;
    }
}
