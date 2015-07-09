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
import alien4cloud.audit.annotation.Audit;
import alien4cloud.cloud.CloudService;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.application.DeploymentSetupMatchInfo;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.paas.exception.MaintenanceModeException;
import alien4cloud.paas.exception.PaaSDeploymentException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.CloudRole;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
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
    private DeploymentSetupService deploymentSetupService;

    /**
     * Trigger deployment of the application on the current configured PaaS.
     *
     * @param deployApplicationRequest application details for deployment (applicationId + deploymentProperties)
     * @return An empty rest response.
     */
    @ApiOperation(value = "Deploys the application on the configured Cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/deployment", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<Void> deploy(@Valid @RequestBody DeployApplicationRequest deployApplicationRequest) throws CloudDisabledException {

        // Application environment : get an check right on the environment
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(deployApplicationRequest.getApplicationId(),
                deployApplicationRequest.getApplicationEnvironmentId());
        Application application = applicationService.checkAndGetApplication(deployApplicationRequest.getApplicationId());
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }

        // Get Application version
        ApplicationVersion version = applicationVersionService.getVersionByIdOrDefault(environment.getApplicationId(), environment.getCurrentVersionId());

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
        DeploymentSetupMatchInfo deploymentSetupMatchInfo = deploymentSetupService.preProcessTopologyAndMatch(topology, environment, version);
        if (!deploymentSetupMatchInfo.isValid()) {
            throw new InvalidDeploymentSetupException("Application [" + application.getId() + "] is not deployable on the cloud [" + cloud.getId()
                    + "] because it contains unmatchable resources");
        }
        try {
            deploymentService.deployTopology(topology, application, deploymentSetupMatchInfo.getDeploymentSetup(), environment.getCloudId());
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
    @Audit
    public RestResponse<Void> undeploy(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, applicationEnvironmentId);
        Application application = applicationService.checkAndGetApplication(applicationId);
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
        ApplicationVersion version = applicationVersionService.getVersionByIdOrDefault(application.getId(), environment.getCurrentVersionId());
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
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        // when a user is application manager, he can manipulate environment
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER,
                    ApplicationEnvironmentRole.APPLICATION_USER);
        }
        Deployment deployment = deploymentService.getActiveDeployment(environment.getId());
        return RestResponseBuilder.<Deployment> builder().data(deployment).build();
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
                if (AuthorizationUtil.hasAuthorizationForEnvironment(env, ApplicationEnvironmentRole.values())) {
                    DeploymentStatus status = DeploymentStatus.UNKNOWN;
                    try {
                        status = applicationEnvironmentService.getStatus(env);
                    } catch (Exception e) {
                        log.debug("Getting status for the environment <" + env.getId() + "> failed because the associated cloud <" + env.getCloudId()
                                + "> seems disabled. Returned status is UNKNOWN.", e);
                    }
                    environmentStatuses.put(env.getId(), new EnvironmentStatusDTO(env.getName(), status));
                }
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
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.values());
        }

        Deployment deployment = applicationEnvironmentService.getActiveDeployment(environment.getId());
        final DeferredResult<RestResponse<Map<String, Map<String, InstanceInformation>>>> instancesDeferredResult = new DeferredResult<>(5L * 60L * 1000L);
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

    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment/maintenance", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> switchMaintenanceModeOn(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);
        try {
            deploymentService.switchMaintenanceMode(environment.getId(), true);
        } catch (CloudDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        } catch (MaintenanceModeException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.MAINTENANCE_MODE_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment/maintenance", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> switchMaintenanceModeOff(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);
        try {
            deploymentService.switchMaintenanceMode(environment.getId(), false);
        } catch (CloudDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        } catch (MaintenanceModeException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.MAINTENANCE_MODE_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment/{nodeTemplateId}/{instanceId}/maintenance", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> switchInstanceMaintenanceModeOn(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @PathVariable String nodeTemplateId, @PathVariable String instanceId) {
        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);
        try {
            deploymentService.switchInstanceMaintenanceMode(environment.getId(), nodeTemplateId, instanceId, true);
        } catch (CloudDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        } catch (MaintenanceModeException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.MAINTENANCE_MODE_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment/{nodeTemplateId}/{instanceId}/maintenance", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> switchInstanceMaintenanceModeOff(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @PathVariable String nodeTemplateId, @PathVariable String instanceId) {
        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);
        try {
            deploymentService.switchInstanceMaintenanceMode(environment.getId(), nodeTemplateId, instanceId, false);
        } catch (CloudDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        } catch (MaintenanceModeException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.MAINTENANCE_MODE_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    private ApplicationEnvironment getAppEnvironmentAndCheckAuthorization(String applicationId, String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);

        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
        return environment;
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
    @Audit
    public RestResponse<Void> scale(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId, @PathVariable String nodeTemplateId,
            @RequestParam int instances) {

        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);

        try {
            deploymentService.scale(environment.getId(), nodeTemplateId, instances);
        } catch (CloudDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        } catch (PaaSDeploymentException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.SCALING_ERROR.getCode(), e.getMessage())).build();
        }

        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Get the deployment setup for an application", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment-setup", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<DeploymentSetupMatchInfo> getDeploymentSetup(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId)
            throws CloudDisabledException {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
        return RestResponseBuilder.<DeploymentSetupMatchInfo> builder()
                .data(deploymentSetupService.getDeploymentSetupMatchInfo(application.getId(), applicationEnvironmentId)).build();
    }

    /**
     * Update application's deployment setup
     *
     * @param applicationId The application id.
     * @return nothing if success, error will be handled in global exception strategy
     */
    @ApiOperation(value = "Updates by merging the given request into the given application's deployment setup.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId}/environments/{applicationEnvironmentId}/deployment-setup", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<?> updateDeploymentSetup(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @RequestBody UpdateDeploymentSetupRequest updateRequest) throws CloudDisabledException {

        Application application = applicationService.checkAndGetApplication(applicationId);
        // check rights on related environment
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }
        ApplicationVersion version = applicationVersionService.getVersionByIdOrDefault(applicationId, environment.getCurrentVersionId());
        Topology topology = topologyServiceCore.getMandatoryTopology(version.getTopologyId());

        DeploymentSetup deploymentSetup = deploymentSetupService.getOrFail(version, environment);
        ReflectionUtil.mergeObject(updateRequest, deploymentSetup);
        if (deploymentSetup.getInputProperties() != null) {
            // If someone modified the input properties, must validate them
            try {
                deploymentSetupService.validateInputProperties(deploymentSetup, topology);
            } catch (ConstraintViolationException e) {
                return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                        .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                        .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
            }
        }
        alienDAO.save(deploymentSetup);
        return RestResponseBuilder.<Void> builder().build();
    }
}
