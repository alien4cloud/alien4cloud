package alien4cloud.rest.application;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

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
import alien4cloud.application.InvalidDeploymentSetupException;
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
import alien4cloud.security.ApplicationEnvironmentRole;
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

    // @Deprecated
    // @ApiOperation(value = "Set the cloud to use by default in order to deploy the application.", notes =
    // "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    // @RequestMapping(value = "/{applicationId:.+}/cloud", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces =
    // MediaType.APPLICATION_JSON_VALUE)
    // public RestResponse<Void> setCloud(@PathVariable String applicationId, @RequestBody UpdateApplicationCloudRequest updateApplicationCloudRequest) {
    //
    // // recover an check basic rights on the underlying application
    // Application application = applicationService.checkAndGetApplication(applicationId);
    //
    // // Application environment : get an check right on the environment
    // ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), updateApplicationCloudRequest.getApplicationEnvironmentId());
    // AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
    //
    // // set the cloud to this environment
    // String cloudId = updateApplicationCloudRequest.getCloudId();
    // environment.setCloudId(cloudId);
    // alienDAO.save(environment);
    //
    // // Application version : check right on the version
    // ApplicationVersion version = getVersionByIdOrDefault(application.getId(), environment.getCurrentVersionId());
    //
    // // Configure and save the deployment setup
    // DeploymentSetup deploymentSetup = getDeploymentSetup(application);
    // Cloud cloud = cloudService.getMandatoryCloud(cloudId);
    // deploymentSetupService.generateCloudResourcesMapping(deploymentSetup, topologyServiceCore.getMandatoryTopology(version.getTopologyId()), cloud, true);
    // deploymentSetupService.generatePropertyDefinition(deploymentSetup, cloud);
    // alienDAO.save(deploymentSetup);
    // return RestResponseBuilder.<Void> builder().build();
    // }

    /**
     * Trigger deployment of the application on the current configured PaaS.
     *
     * @param deployApplicationRequest application details for deployment (applicationId + deploymentProperties)
     * @return An empty rest response.
     */
    @ApiOperation(value = "Deploys the application on the configured Cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/deployment", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> deploy(@Valid @RequestBody DeployApplicationRequest deployApplicationRequest) {

        Application application = applicationService.checkAndGetApplication(deployApplicationRequest.getApplicationId());

        // Application environment : get an check right on the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(deployApplicationRequest.getApplicationId(),
                deployApplicationRequest.getApplicationEnvironmentId());
        AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);

        // Application version : check right on the version
        ApplicationVersion version = getVersionByIdOrDefault(environment.getApplicationId(), environment.getCurrentVersionId());

        // check that the environment is not already deployed
        // One environment => One deployment deployed at a time
        try {
            boolean isEnvironmentDeployed = applicationEnvironmentService.isDeployed(environment.getId());
            if (isEnvironmentDeployed) {
                return RestResponseBuilder
                        .<Void> builder()
                        .error(new RestError(RestErrorCode.APPLICATION_DEPLOYMENT_ERROR.getCode(), "The environment with id <" + environment.getCloudId()
                                + "> is already deployed")).build();
            }
        } catch (CloudDisabledException e) {
            return RestResponseBuilder
                    .<Void> builder()
                    .error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), "Cloud with id <" + environment.getCloudId()
                            + "> is disabled or not found")).build();
        }

        // get the cloud from the environment and check rights
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
            deploymentService.deployTopology(topology, application, deploymentSetup);
        } catch (CloudDisabledException e) {
            return RestResponseBuilder
                    .<Void> builder()
                    .error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), "Cloud with id <" + environment.getCloudId()
                            + "> is disabled or not found")).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Trigger un-deployment of the application for a given environment on the current configured PaaS.
     *
     * @param applicationId The id of the application to undeploy.
     * @return An empty rest response.
     */
    @ApiOperation(value = "Un-Deploys the application on the configured PaaS.", notes = "The logged-in user must have the [ APPLICATION_MANAGER ] role for this application. Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> undeploy(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {

        Application application = applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);

        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        ApplicationVersion version = getVersionByIdOrDefault(application.getId(), environment.getCurrentVersionId());
        try {
            boolean isEnvironmentDeployed = applicationEnvironmentService.isDeployed(environment.getId());
            if (isEnvironmentDeployed) {
                DeploymentSetup deploymentSetup = deploymentSetupService.getOrFail(version, environment);
                deploymentService.undeployTopology(deploymentSetup);
            }
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
    @ApiOperation(value = "Get active deployment for the given application on the given cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/active-deployment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Deployment> getActiveDeployment(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        Deployment deployment = deploymentService.getActiveDeployment(environment.getId());
        return RestResponseBuilder.<Deployment> builder().data(deployment).build();
    }

    /**
     * Get the current status of the deployment for the given application.
     * 
     * @param applicationId the id of the application to be deployed.
     * @param applicationEnvironmentId the environment for which to get the status
     * @return A {@link RestResponse} that contains the application's current {@link DeploymentStatus}.
     */
    @ApiOperation(value = "Get the current status of the application and an environment on the PaaS.", notes = "Returns the current status of the application on the PaaS it is deployed."
            + " Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<DeploymentStatus> getDeploymentStatus(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        return RestResponseBuilder.<DeploymentStatus> builder().data(getApplicationDeploymentStatus(application, applicationEnvironmentId)).build();
    }

    @ApiOperation(value = "Get the current statuses of an application list on the PaaS for all environments.", notes = "Returns the current status of an application list from the PaaS it is deployed on for all environments."
            + " Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/statuses", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Map<String, Map<String, DeploymentStatus>>> getApplicationsStatuses(@RequestBody List<String> applicationIds) {
        Map<String, Map<String, DeploymentStatus>> statuses = Maps.newHashMap();
        Application application = null;
        Map<String, DeploymentStatus> ennvironmentStatuses = Maps.newHashMap();
        ApplicationEnvironment[] environments;
        for (String applicationId : applicationIds) {
            application = applicationService.checkAndGetApplication(applicationId);
            // get all environments status for the current application
            environments = applicationEnvironmentService.getByApplicationId(application.getId());
            for (ApplicationEnvironment env : environments) {
                try {
                    ennvironmentStatuses.put(env.getId(), applicationEnvironmentService.getStatus(env));
                } catch (CloudDisabledException e) {
                    log.debug("Getting status for the environment <" + env.getId() + "> failed because the associated cloud <" + env.getCloudId()
                            + "> seems disabled. Returned status is UNKNOWN.", e);
                    ennvironmentStatuses.put(env.getId(), DeploymentStatus.UNKNOWN);
                }
            }
            statuses.put(applicationId, ennvironmentStatuses);
            ennvironmentStatuses = Maps.newHashMap();
        }
        return RestResponseBuilder.<Map<String, Map<String, DeploymentStatus>>> builder().data(statuses).build();
    }

    private DeploymentStatus getApplicationDeploymentStatus(Application application, String applicationEnvironmentId) {
        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.values());
        ApplicationVersion version = getVersionByIdOrDefault(application.getId(), environment.getCurrentVersionId());

        DeploymentStatus deploymentStatus;
        if (version.getTopologyId() == null) {
            deploymentStatus = DeploymentStatus.UNDEPLOYED;
        } else {
            try {
                deploymentStatus = applicationEnvironmentService.getStatus(environment);
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
    @ApiOperation(value = "Get detailed informations for every instances of every node of the application on the PaaS.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment/informations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Map<String, Map<Integer, InstanceInformation>>> getInstanceInformation(@PathVariable String applicationId,
            @PathVariable String applicationEnvironmentId) {

        Application application = applicationService.checkAndGetApplication(applicationId);
        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        ApplicationVersion version = getVersionByIdOrDefault(application.getId(), environment.getCurrentVersionId());
        try {
            return RestResponseBuilder.<Map<String, Map<Integer, InstanceInformation>>> builder()
                    .data(deploymentService.getInstancesInformation(version.getTopologyId(), environment.getCloudId(), environment.getId())).build();
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
            + " Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/scale/{nodeTemplateId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> scale(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId, @PathVariable String nodeTemplateId,
            @RequestParam int instances) {

        Application application = applicationService.checkAndGetApplication(applicationId);

        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);

        try {
            deploymentService.scale(environment.getId(), nodeTemplateId, instances);
        } catch (CloudDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Match the topology of a given application to a cloud, get all available resources for all matchable elements of the topology", notes = "Returns the detailed informations of the application on the PaaS it is deployed."
            + " Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/cloud-resources", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<CloudResourceTopologyMatchResult> matchCloudResources(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {

        Application application = applicationService.checkAndGetApplication(applicationId);

        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        ApplicationVersion version = getVersionByIdOrDefault(application.getId(), environment.getCurrentVersionId());

        // Application application = applicationService.getOrFail(applicationId);
        // AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.values());
        //
        // ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
        // ApplicationVersion[] versions = applicationVersionService.getByApplicationId(application.getId());
        // // get the topology from the version and the cloud from the environment.
        // ApplicationVersion version = versions[0];
        // ApplicationEnvironment environment = environments[0];

        Topology topology = topologyServiceCore.getMandatoryTopology(version.getTopologyId());
        if (environment.getCloudId() == null) {
            throw new InvalidArgumentException("Environment [" + applicationEnvironmentId + "] for application [" + application.getName()
                    + "] does not have any cloud assigned");
        }
        Cloud cloud = cloudService.getMandatoryCloud(environment.getCloudId());
        CloudResourceMatcherConfig cloudResourceMatcherConfig = cloudService.findCloudResourceMatcherConfig(cloud);
        return RestResponseBuilder
                .<CloudResourceTopologyMatchResult> builder()
                .data(cloudResourceMatcherService.matchTopology(topology, cloud, cloudResourceMatcherConfig,
                        topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true))).build();
    }

    @ApiOperation(value = "Get the deployment setup for an application", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment-setup", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<DeploymentSetup> getDeploymentSetup(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        return RestResponseBuilder.<DeploymentSetup> builder().data(getDeploymentSetup(application, applicationEnvironmentId)).build();
    }

    private DeploymentSetup getDeploymentSetup(Application application, String applicationEnvironmentId) {

        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        ApplicationVersion version = getVersionByIdOrDefault(application.getId(), environment.getCurrentVersionId());

        DeploymentSetup deploymentSetup = deploymentSetupService.get(version, environment);
        if (deploymentSetup == null) {
            deploymentSetup = deploymentSetupService.createOrFail(version, environment);
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
    @ApiOperation(value = "Updates by merging the given request into the given application's deployment setup.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment-setup", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> updateDeploymentSetup(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @RequestBody UpdateDeploymentSetupRequest updateRequest) {

        Application application = applicationService.checkAndGetApplication(applicationId);
        // check rights on related environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForApplication(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);

        DeploymentSetup setup = getDeploymentSetup(application, applicationEnvironmentId);
        ReflectionUtil.mergeObject(updateRequest, setup);
        alienDAO.save(setup);
        return RestResponseBuilder.<Void> builder().build();
    }

    private ApplicationVersion getVersionByIdOrDefault(String applicationId, String applicationVersionId) {
        ApplicationVersion version = null;
        if (applicationVersionId == null) {
            ApplicationVersion[] versions = applicationVersionService.getByApplicationId(applicationId);
            version = versions[0];
        } else {
            version = applicationVersionService.getOrFail(applicationVersionId);
        }
        return version;
    }

    private ApplicationEnvironment getEnvironmentByIdOrDefault(String applicationId, String applicationEnvironmentId) {
        ApplicationEnvironment environment = null;
        if (applicationEnvironmentId == null) {
            ApplicationEnvironment[] applicationEnvironments = applicationEnvironmentService.getByApplicationId(applicationId);
            environment = applicationEnvironments[0];
        } else {
            environment = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        }
        return environment;
    }
}
