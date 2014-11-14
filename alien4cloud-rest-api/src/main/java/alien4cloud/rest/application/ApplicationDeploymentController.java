package alien4cloud.rest.application;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import alien4cloud.application.InvalidDeploymentSetupException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.application.DeploymentSetupService;
import alien4cloud.cloud.CloudResourceMatcherService;
import alien4cloud.cloud.CloudResourceTopologyMatchResult;
import alien4cloud.cloud.CloudService;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.CloudRole;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.utils.ReflectionUtil;

import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Slf4j
@RestController
@RequestMapping("/rest/applications")
@Api(value = "", description = "Manage opertions on deployed application.")
public class ApplicationDeploymentController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private CloudService cloudService;
    @Resource
    private DeploymentService deploymentService;
    @Resource
    private CloudResourceMatcherService cloudResourceMatcherService;
    @Resource
    private DeploymentSetupService deploymentSetupService;

    @ApiOperation(value = "Set the cloud to use by default in order to deploy the application.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{applicationId:.+}/cloud", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> setCloud(@PathVariable String applicationId, @RequestBody String cloudId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.DEPLOYMENT_MANAGER);

        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(applicationId);
        environments[0].setCloudId(cloudId);
        alienDAO.save(environments[0]);
        ApplicationVersion[] versions = applicationVersionService.getByApplicationId(application.getId());
        ApplicationVersion version = versions[0];
        DeploymentSetup deploymentSetup = getDeploymentSetup(application);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        deploymentSetupService.generateCloudResourcesMapping(deploymentSetup, topologyServiceCore.getMandatoryTopology(version.getTopologyId()), cloud, true);
        deploymentSetupService.generatePropertyDefinition(deploymentSetup, cloud);
        alienDAO.save(deploymentSetup);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Trigger deployment of the application on the current configured PaaS.
     *
     * @param deployApplicationRequest application details for deployment (applicationId + deploymentProperties)
     * @return An empty rest response.
     */
    @ApiOperation(value = "Deploys the application on the configured Cloud.")
    @RequestMapping(value = "/deployment", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> deploy(@RequestBody DeployApplicationRequest deployApplicationRequest) {
        Application application = applicationService.getOrFail(deployApplicationRequest.getApplicationId());
        // TODO rather check that the user is authorized to deploy the current environment.
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.DEPLOYMENT_MANAGER);

        // Get the application environment associated with the application (in the current version of A4C there is just a single environment.
        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
        ApplicationVersion[] versions = applicationVersionService.getByApplicationId(application.getId());

        // TODO check that the environment is not already deployed.

        // get the topology from the version and the cloud from the environment.
        ApplicationVersion version = versions[0];
        ApplicationEnvironment environment = environments[0];

        Cloud cloud = cloudService.getMandatoryCloud(environment.getCloudId());
        AuthorizationUtil.checkAuthorizationForCloud(cloud, CloudRole.values());

        Topology topology = topologyServiceCore.getMandatoryTopology(version.getTopologyId());
        if (environment.getCloudId() == null) {
            throw new InvalidArgumentException("Application [" + application.getId() + "] contains an environment with no cloud assigned");
        }
        DeploymentSetup deploymentSetup = deploymentSetupService.getOrFail(version, environment);
        if (!deploymentSetupService.generateCloudResourcesMapping(deploymentSetup, topology, cloud, true)) {
            throw new InvalidDeploymentSetupException("Application [" + application.getId() + "] is not deployable on the cloud [" + cloud.getId()
                    + "] because it contains unmatchable resources");
        }
        try {
            deploymentService.deployTopology(topology, environment.getCloudId(), application, deploymentSetup);
        } catch (CloudDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Trigger un-deployment of the application on the current configured PaaS.
     *
     * @param applicationId The id of the application to undeploy.
     * @return An empty rest response.
     */
    @ApiOperation(value = "Un-Deploys the application on the configured PaaS.", notes = "The logged-in user must have the application manager role for this application. Application role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/deployment", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> undeploy(@PathVariable String applicationId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.DEPLOYMENT_MANAGER);

        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
        ApplicationVersion[] versions = applicationVersionService.getByApplicationId(application.getId());
        // get the topology from the version and the cloud from the environment.
        ApplicationVersion version = versions[0];
        ApplicationEnvironment environment = environments[0];

        try {
            deploymentService.undeployTopology(version.getTopologyId(), environment.getCloudId());
        } catch (CloudDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Get only the active deployment for the given application on the given cloud
     *
     * @param applicationId id of the topology
     * @return the active deployment
     */
    @ApiOperation(value = "Get active deployment for the given application on the given cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/active-deployment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Deployment> getActiveDeployment(@PathVariable String applicationId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());

        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
        ApplicationVersion[] versions = applicationVersionService.getByApplicationId(application.getId());
        // get the topology from the version and the cloud from the environment.
        ApplicationVersion version = versions[0];
        ApplicationEnvironment environment = environments[0];

        Deployment deployment = deploymentService.getActiveDeployment(version.getTopologyId(), environment.getCloudId());
        return RestResponseBuilder.<Deployment> builder().data(deployment).build();
    }

    /**
     * Get the current status of the deployment for the given application.
     *
     * @param applicationId The id of the application to be deployed.
     * @return A {@link RestResponse} that contains the application's current {@link DeploymentStatus}.
     */
    @ApiOperation(value = "Get the current status of the application on the PaaS.", notes = "Returns the current status of the application on the PaaS it is deployed."
            + " Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/deployment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<DeploymentStatus> getDeploymentStatus(@PathVariable String applicationId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        return RestResponseBuilder.<DeploymentStatus> builder().data(getApplicationDeploymentStatus(application)).build();
    }

    @ApiOperation(value = "Get the current statuses of an application list on the PaaS.", notes = "Returns the current status of an application list from the PaaS it is deployed on."
            + " Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/statuses", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Map<String, DeploymentStatus>> getApplicationsStatuses(@RequestBody List<String> applicationIds) {
        Map<String, DeploymentStatus> statuses = Maps.newHashMap();
        for (String applicationId : applicationIds) {
            Application application = applicationService.getOrFail(applicationId);
            AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
            statuses.put(applicationId, getApplicationDeploymentStatus(application));
        }
        return RestResponseBuilder.<Map<String, DeploymentStatus>> builder().data(statuses).build();
    }

    private DeploymentStatus getApplicationDeploymentStatus(Application application) {
        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
        ApplicationVersion[] versions = applicationVersionService.getByApplicationId(application.getId());
        // get the topology from the version and the cloud from the environment.
        ApplicationVersion version = versions[0];
        ApplicationEnvironment environment = environments[0];

        DeploymentStatus deploymentStatus;
        if (version.getTopologyId() == null) {
            deploymentStatus = DeploymentStatus.UNDEPLOYED;
        } else {
            try {
                deploymentStatus = deploymentService.getDeploymentStatus(version.getTopologyId(), environment.getCloudId());
            } catch (CloudDisabledException e) {
                log.debug("Getting status for topology failed because plugin wasn't found. Returned status is undeployed.", e);
                deploymentStatus = DeploymentStatus.UNDEPLOYED;
            }
        }
        return deploymentStatus;
    }

    /**
     * Get detailed informations for every instances of every node of the application on the PaaS.
     *
     * @param applicationId The id of the application to be deployed.
     * @return A {@link RestResponse} that contains detailed informations (See {@link InstanceInformation}) of the application on the PaaS it is deployed.
     */
    @ApiOperation(value = "Get detailed informations for every instances of every node of the application on the PaaS.", notes = "Returns the detailed informations of the application on the PaaS it is deployed."
            + " Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/deployment/informations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Map<String, Map<Integer, InstanceInformation>>> getInstanceInformation(@PathVariable String applicationId) {
        Application application = applicationService.getOrFail(applicationId);

        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());

        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
        ApplicationVersion[] versions = applicationVersionService.getByApplicationId(application.getId());
        // get the topology from the version and the cloud from the environment.
        ApplicationVersion version = versions[0];
        ApplicationEnvironment environment = environments[0];

        try {
            return RestResponseBuilder.<Map<String, Map<Integer, InstanceInformation>>> builder()
                    .data(deploymentService.getInstancesInformation(version.getTopologyId(), environment.getCloudId())).build();
        } catch (CloudDisabledException e) {
            log.error("Cannot get instance informations as topology plugin cannot be found.", e);
        }

        return RestResponseBuilder.<Map<String, Map<Integer, InstanceInformation>>> builder().build();
    }

    /**
     * Scale an application on a particular node.
     *
     * @param applicationId The id of the application to be scaled.
     * @param nodeTemplateId The id of the node template to be scaled.
     * @param instances The instances number to be scaled up (if > 0)/ down (if < 0)
     * @return A {@link RestResponse} that contains the application's current {@link DeploymentStatus}.
     */
    @ApiOperation(value = "Scale the application on a particular node.", notes = "Returns the detailed informations of the application on the PaaS it is deployed."
            + " Application role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/scale/{nodeTemplateId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> scale(@PathVariable String applicationId, @PathVariable String nodeTemplateId, @RequestParam int instances) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.DEPLOYMENT_MANAGER);

        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
        ApplicationVersion[] versions = applicationVersionService.getByApplicationId(application.getId());
        // get the topology from the version and the cloud from the environment.
        ApplicationVersion version = versions[0];
        ApplicationEnvironment environment = environments[0];

        try {
            deploymentService.scale(version.getTopologyId(), environment.getCloudId(), nodeTemplateId, instances);
        } catch (CloudDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Match the topology of a given application to a cloud, get all available resources for all matchable elements of the topology")
    @RequestMapping(value = "/{applicationId}/cloud-resources", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<CloudResourceTopologyMatchResult> matchCloudResources(@PathVariable String applicationId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.DEPLOYMENT_MANAGER);

        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
        ApplicationVersion[] versions = applicationVersionService.getByApplicationId(application.getId());
        // get the topology from the version and the cloud from the environment.
        ApplicationVersion version = versions[0];
        ApplicationEnvironment environment = environments[0];
        Topology topology = topologyServiceCore.getMandatoryTopology(version.getTopologyId());
        if (environment.getCloudId() == null) {
            throw new InvalidArgumentException("Application [" + application.getName() + "] does not have any cloud assigned");
        }
        Cloud cloud = cloudService.getMandatoryCloud(environment.getCloudId());
        CloudResourceMatcherConfig cloudResourceMatcherConfig = cloudService.findCloudResourceMatcherConfig(cloud);
        return RestResponseBuilder
                .<CloudResourceTopologyMatchResult> builder()
                .data(cloudResourceMatcherService.matchTopology(topology, cloud, cloudResourceMatcherConfig,
                        topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true))).build();
    }

    @ApiOperation(value = "Get the deployment setup for an application")
    @RequestMapping(value = "/{applicationId}/deployment-setup", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<DeploymentSetup> getDeploymentSetup(@PathVariable String applicationId) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.DEPLOYMENT_MANAGER);
        return RestResponseBuilder.<DeploymentSetup> builder().data(getDeploymentSetup(application)).build();
    }

    private DeploymentSetup getDeploymentSetup(Application application) {
        ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
        ApplicationVersion[] versions = applicationVersionService.getByApplicationId(application.getId());
        // get the topology from the version and the cloud from the environment.
        ApplicationVersion version = versions[0];
        ApplicationEnvironment environment = environments[0];
        DeploymentSetup deploymentSetup = deploymentSetupService.get(version, environment);
        if (deploymentSetup == null) {
            deploymentSetup = deploymentSetupService.create(version, environment);
        }
        if (environment.getCloudId() != null) {
            Cloud cloud = cloudService.getMandatoryCloud(environment.getCloudId());
            deploymentSetupService.generateCloudResourcesMapping(deploymentSetup, topologyServiceCore.getMandatoryTopology(version.getTopologyId()), cloud,
                    true);
        }
        return deploymentSetup;
    }

    /**
     * Update application's deployment setup
     *
     * @param applicationId The application id.
     * @return nothing if success, error will be handled in global exception strategy
     */
    @ApiOperation(value = "Updates by merging the given request into the given application's deployment setup.")
    @RequestMapping(value = "/{applicationId}/deployment-setup", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> updateDeploymentSetup(@PathVariable String applicationId, @RequestBody UpdateDeploymentSetupRequest updateRequest) {
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);
        DeploymentSetup setup = getDeploymentSetup(application);
        ReflectionUtil.mergeObject(updateRequest, setup);
        alienDAO.save(setup);
        return RestResponseBuilder.<Void> builder().build();
    }
}
