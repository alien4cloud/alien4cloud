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
import org.springframework.web.context.request.async.DeferredResult;

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
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.IPaaSCallback;
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
import alien4cloud.utils.ReflectionUtil;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
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

    /**
     * Trigger deployment of the application on the current configured PaaS.
     *
     * @param deployApplicationRequest application details for deployment (applicationId + deploymentProperties)
     * @return An empty rest response.
     */
    @ApiOperation(value = "Deploys the application on the configured Cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/deployment", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> deploy(@Valid @RequestBody DeployApplicationRequest deployApplicationRequest) {

        // Application environment : get an check right on the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(deployApplicationRequest.getApplicationId(),
                deployApplicationRequest.getApplicationEnvironmentId());
        Application application = applicationService.checkAndGetApplication(deployApplicationRequest.getApplicationId());
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }

        // Application version : check right on the version
        ApplicationVersion version = getVersionByIdOrDefault(environment.getApplicationId(), environment.getCurrentVersionId());

        // check that the environment is not already deployed
        // One environment => One deployment deployed at a time
        boolean isEnvironmentDeployed = applicationEnvironmentService.isDeployed(environment.getId());
        if (isEnvironmentDeployed) {
            return RestResponseBuilder
                    .<Void> builder()
                    .error(new RestError(RestErrorCode.APPLICATION_DEPLOYMENT_ERROR.getCode(), "The environment with id <" + environment.getId()
                            + "> is already deployed")).build();
        }

        // get the cloud from the environment and check rights
        if (environment.getCloudId() == null) {
            return RestResponseBuilder
                    .<Void> builder()
                    .error(new RestError(RestErrorCode.INVALID_APPLICATION_ENVIRONMENT_ERROR.getCode(), "The environment with id <" + environment.getId()
                            + "> has no declared cloud.")).build();
        }

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
            deploymentService.deployTopology(topology, application, deploymentSetup, environment.getCloudId());
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

        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(applicationId, applicationEnvironmentId);
        Application application = applicationService.checkAndGetApplication(applicationId);
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
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
        // when a user is application manager, he can manipulate environment
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
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
    @ApiOperation(value = "Get the current status of an application environment on the PaaS.", notes = "Returns the current status of the application on the PaaS it is deployed.")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<RestResponse<DeploymentStatus>> getDeploymentStatus(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        final DeferredResult<RestResponse<DeploymentStatus>> statusResult = new DeferredResult<>();
        Futures.addCallback(getApplicationDeploymentStatus(application, applicationEnvironmentId), new FutureCallback<DeploymentStatus>() {
            @Override
            public void onSuccess(DeploymentStatus result) {
                statusResult.setResult(RestResponseBuilder.<DeploymentStatus> builder().data(result).build());
            }

            @Override
            public void onFailure(Throwable t) {
                statusResult.setErrorResult(t);
            }
        });
        return statusResult;
    }

    private ListenableFuture<DeploymentStatus> getApplicationDeploymentStatus(Application application, String applicationEnvironmentId) {
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.values());
        }

        final SettableFuture<DeploymentStatus> statusSettableFuture = SettableFuture.create();
        Deployment deployment = applicationEnvironmentService.getActiveDeployment(environment.getId());
        if (deployment == null) { // if there is no topology associated with the version it could not have been deployed.
            statusSettableFuture.set(DeploymentStatus.UNDEPLOYED);
        } else {
            try {
                deploymentService.getDeploymentStatus(deployment, new IPaaSCallback<DeploymentStatus>() {
                    @Override
                    public void onSuccess(DeploymentStatus data) {
                        statusSettableFuture.set(data);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        statusSettableFuture.setException(throwable);
                    }
                });

            } catch (CloudDisabledException e) {
                log.debug("Getting status for topology failed because cloud is disabled. Returned status is unknown", e);
                statusSettableFuture.set(DeploymentStatus.UNKNOWN);

            }
        }

        return statusSettableFuture;
    }

    @ApiOperation(value = "Get the deployment status for the environements that the current user is allowed to see for a given application.", notes = "Returns the current status of an application list from the PaaS it is deployed on for all environments.")
    @RequestMapping(value = "/statuses", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Map<String, Map<String, EnvironmentStatusDTO>>> getApplicationsStatuses(@RequestBody List<String> applicationIds) {
        Map<String, Map<String, EnvironmentStatusDTO>> statuses = Maps.newHashMap();

        for (String applicationId : applicationIds) {
            Map<String, EnvironmentStatusDTO> environmentStatuses = Maps.newHashMap();
            Application application = applicationService.checkAndGetApplication(applicationId);
            // get all environments status for the current application
            ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
            for (ApplicationEnvironment env : environments) {
                DeploymentStatus status = DeploymentStatus.UNKNOWN;
                try {
                    status = applicationEnvironmentService.getStatus(env);
                } catch (CloudDisabledException e) {
                    log.debug("Getting status for the environment <" + env.getId() + "> failed because the associated cloud <" + env.getCloudId()
                            + "> seems disabled. Returned status is UNKNOWN.", e);
                }
                environmentStatuses.put(env.getId(), new EnvironmentStatusDTO(env.getName(), status));

            }
            statuses.put(applicationId, environmentStatuses);
        }
        return RestResponseBuilder.<Map<String, Map<String, EnvironmentStatusDTO>>> builder().data(statuses).build();
    }

    /**
     * Get detailed informations for every instances of every node of the application on the PaaS.
     *
     * @param applicationId The id of the application to be deployed.
     * @return A {@link RestResponse} that contains detailed informations (See {@link InstanceInformation}) of the application on the PaaS it is deployed.
     */
    @ApiOperation(value = "Get detailed informations for every instances of every node of the application on the PaaS.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ APPLICATION_USER | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment/informations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<RestResponse<Map<String, Map<String, InstanceInformation>>>> getInstanceInformation(@PathVariable String applicationId,
            @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.values());
        }

        Deployment deployment = applicationEnvironmentService.getActiveDeployment(environment.getId());
        final DeferredResult<RestResponse<Map<String, Map<String, InstanceInformation>>>> instancesDeferredResult = new DeferredResult<>();
        if (deployment == null) { // if there is no topology associated with the version it could not have been deployed.
            instancesDeferredResult.setResult(RestResponseBuilder.<Map<String, Map<String, InstanceInformation>>> builder().build());
        } else {
            try {
                deploymentService.getInstancesInformation(deployment, new IPaaSCallback<Map<String, Map<String, InstanceInformation>>>() {
                    @Override
                    public void onSuccess(Map<String, Map<String, InstanceInformation>> data) {
                        instancesDeferredResult.setResult(RestResponseBuilder.<Map<String, Map<String, InstanceInformation>>> builder().data(data).build());
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        instancesDeferredResult.setErrorResult(throwable);
                    }
                });
            } catch (CloudDisabledException e) {
                log.error("Cannot get instance informations as topology plugin cannot be found.", e);
                instancesDeferredResult.setResult(RestResponseBuilder.<Map<String, Map<String, InstanceInformation>>> builder().build());
            }
        }
        return instancesDeferredResult;
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
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }

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
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
        ApplicationVersion version = getVersionByIdOrDefault(application.getId(), environment.getCurrentVersionId());

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
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
        return RestResponseBuilder.<DeploymentSetup> builder().data(getDeploymentSetup(application, applicationEnvironmentId)).build();
    }

    /**
     * Get the deployment setup
     * (environment right check done before method call)
     * 
     * @param application
     * @param applicationEnvironmentId
     * @return
     */
    private DeploymentSetup getDeploymentSetup(Application application, String applicationEnvironmentId) {

        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
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
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }

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
