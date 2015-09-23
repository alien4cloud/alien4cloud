package alien4cloud.rest.deployment;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.AlienConstants;
import alien4cloud.deployment.DeploymentTopologyService;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.rest.application.model.SetLocationPoliciesRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Slf4j
@RestController
@RequestMapping("/rest/applications/{appId}/environments/{environmentId}/deployment-topology")
@Api(value = "", description = "Manage configuration of an application before deploying it.")
public class DeploymentConfigurationController {

    @Inject
    private DeploymentTopologyService deploymentTopoService;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService appEnvironmentService;

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
        DeploymentTopology deploymentTopo = deploymentTopoService.getOrCreateDeploymentTopology(environmentId);
        DeploymentTopologyDTO dto = buildDeploymentTopologyDTO(deploymentTopo);
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
    public RestResponse<DeploymentTopology> setLocationPolicies(@PathVariable String appId, @PathVariable String environmentId,
            @RequestBody SetLocationPoliciesRequest request) {
        RestResponseBuilder<DeploymentTopology> responseBuilder = RestResponseBuilder.<DeploymentTopology> builder();

        checkAuthorizations(appId, environmentId);
        DeploymentTopology deploymentTopo = deploymentTopoService.setLocationPolicies(environmentId, request.getOrchestratorId(),
                request.getGroupsToLocations());
        return responseBuilder.data(deploymentTopo).build();
    }

    /**
     * Security check on application and environment
     *
     * @param appId
     * @param environmentId
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

        DeploymentTopologyDTO dto = new DeploymentTopologyDTO();
        dto.setDeploymentTopology(deploymentTopology);
        String locationId = TopologyLocationUtils.getLocationId(deploymentTopology);
        if (StringUtils.isNotBlank(locationId)) {
            dto.getLocationPolicies().put(AlienConstants.GROUP_ALL, locationId);
        }

        return dto;

    }
}
