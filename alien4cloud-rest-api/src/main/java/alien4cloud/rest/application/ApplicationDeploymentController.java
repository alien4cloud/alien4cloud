package alien4cloud.rest.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.validation.Valid;

import alien4cloud.rest.application.model.MonitoredDeploymentDTO;
import org.alien4cloud.alm.deployment.configuration.model.SecretCredentialInfo;
import org.alien4cloud.git.GitLocationDao;
import org.alien4cloud.git.LocalGitManager;
import org.alien4cloud.git.model.GitLocation;
import org.alien4cloud.secret.services.SecretProviderService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.topology.TopologyDTOBuilder;
import org.alien4cloud.tosca.utils.TopologyUtils;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.deployment.DeployService;
import alien4cloud.deployment.DeploymentRuntimeService;
import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.deployment.DeploymentTopologyDTO;
import alien4cloud.deployment.DeploymentTopologyDTOBuilder;
import alien4cloud.deployment.UndeployService;
import alien4cloud.deployment.WorkflowExecutionService;
import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.deployment.model.SecretProviderCredentials;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationTopologyVersion;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.MaintenanceModeException;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.paas.exception.PaaSDeploymentException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.rest.application.model.ApplicationEnvironmentDTO;
import alien4cloud.rest.application.model.DeployApplicationRequest;
import alien4cloud.rest.application.model.EnvironmentStatusDTO;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.User;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.tosca.context.ToscaContextualAspect;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({ "/rest/applications", "/rest/v1/applications", "/rest/latest/applications" })
@Api(value = "", description = "Manage opertions on deployed application.")
public class ApplicationDeploymentController {
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private DeploymentService deploymentService;
    @Inject
    private DeployService deployService;
    @Inject
    private UndeployService undeployService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private DeploymentRuntimeService deploymentRuntimeService;
    @Inject
    private WorkflowExecutionService workflowExecutionService;
    @Inject
    private TopologyDTOBuilder topologyDTOBuilder;
    @Inject
    private ApplicationEnvironmentDTOBuilder dtoBuilder;
    @Inject
    private ApplicationVersionService applicationVersionService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private DeploymentTopologyDTOBuilder deploymentTopologyDTOBuilder;
    @Inject
    private LocalGitManager localGitManager;
    @Inject
    private GitLocationDao gitLocationDao;
    @Inject
    private ToscaContextualAspect toscaContextualAspect;
    @Inject
    private LocationService locationService;
    @Inject
    private SecretProviderService secretProviderService;

    /**
     * Trigger deployment of the application on the current configured PaaS.
     *
     * @param deployApplicationRequest application details for deployment (applicationId + deploymentProperties)
     * @return An empty rest response.
     */
    @ApiOperation(value = "Deploys the application on the configured Cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/deployment", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit(bodyHiddenFields = { "secretProviderCredentials" })
    public RestResponse<?> deploy(@Valid @RequestBody DeployApplicationRequest deployApplicationRequest) {
        String applicationId = deployApplicationRequest.getApplicationId();
        String environmentId = deployApplicationRequest.getApplicationEnvironmentId();
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, environmentId);
        if (!environment.getApplicationId().equals(applicationId)) {
            throw new NotFoundException("Unable to find environment with id <" + environmentId + "> for application <" + applicationId + ">");
        }
        // Security check user must be authorized to deploy the environment (or be application manager)
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);

        // ensure deployment status is sync with underlying orchestrator
        applicationEnvironmentService.getStatus(environment);
        // check that the environment is not already deployed
        boolean isEnvironmentDeployed = applicationEnvironmentService.isDeployed(environment.getId());
        if (isEnvironmentDeployed) {
            throw new AlreadyExistException("Environment with id <" + environmentId + "> for application <" + applicationId + "> is already deployed");
        }

        // prepare the deployment
        ApplicationTopologyVersion topologyVersion = applicationVersionService
                .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
        Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());
        return toscaContextualAspect.execInToscaContext(() -> doDeploy(deployApplicationRequest, application, environment, topology), false, topology);
    }

    private RestResponse<?> doDeploy(DeployApplicationRequest deployApplicationRequest, Application application, ApplicationEnvironment environment,
            Topology topology) {
        DeploymentTopologyDTO deploymentTopologyDTO = deploymentTopologyDTOBuilder.prepareDeployment(topology, application, environment);
        TopologyValidationResult validation = deploymentTopologyDTO.getValidation();

        // if not valid, then return validation errors
        if (!validation.isValid()) {
            return RestResponseBuilder.<TopologyValidationResult> builder()
                    .error(new RestError(RestErrorCode.INVALID_DEPLOYMENT_TOPOLOGY.getCode(), "The deployment topology for the application <"
                            + application.getName() + "> on the environment <" + environment.getName() + "> is not valid."))
                    .data(validation).build();
        }

        User deployer = AuthorizationUtil.getCurrentUser();
        // commit and push the deployment configuration data
        GitLocation location = gitLocationDao.findDeploymentSetupLocation(application.getId(), environment.getId());
        localGitManager.commitAndPush(location, deployer.getUsername(), deployer.getEmail(), "Deployment " + DateTime.now(DateTimeZone.UTC));

        // the request contains secret provider credentials?
        SecretProviderCredentials secretProviderCredentials = null;
        if (deployApplicationRequest.getSecretProviderCredentials() != null && deployApplicationRequest.getSecretProviderPluginName() != null) {
            secretProviderCredentials = new SecretProviderCredentials();
            secretProviderCredentials.setCredentials(deployApplicationRequest.getSecretProviderCredentials());
            secretProviderCredentials.setPluginName(deployApplicationRequest.getSecretProviderPluginName());
        }

        // process with the deployment
        deployService.deploy(deployer, secretProviderCredentials, deploymentTopologyDTO.getTopology(), application);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Trigger un-deployment of the application for a given environment on the current configured PaaS.
     *
     * @param applicationId The id of the application to undeploy.
     * @param applicationEnvironmentId the id of the application environment to undeploy.
     * @return An empty rest response.
     */
    @Deprecated
    @ApiOperation(value = "Un-Deploys the application on the configured PaaS.", notes = "The logged-in user must have the [ APPLICATION_MANAGER ] role for this application. Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/deployment", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> undeploy(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        return doUndeploy(applicationId, applicationEnvironmentId, new SecretProviderConfigurationAndCredentials());
    }

    /**
     * Trigger un-deployment of the application for a given environment on the current configured PaaS.
     *
     * @param applicationId The id of the application to undeploy.
     * @param applicationEnvironmentId the id of the application environment to undeploy.
     * @param secretProviderConfigurationAndCredentials The secret provider configuration and credentials.
     * @return An empty rest response.
     */
    @ApiOperation(value = "Un-Deploys the application on the configured PaaS.", notes = "The logged-in user must have the [ APPLICATION_MANAGER ] role for this application. Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/deployment", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit(bodyHiddenFields = { "credentials" })
    public RestResponse<Void> undeploy(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @ApiParam(value = "The secret provider configuration and credentials.") @RequestBody SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials) {
        return doUndeploy(applicationId, applicationEnvironmentId, secretProviderConfigurationAndCredentials);
    }

    private RestResponse<Void> doUndeploy(String applicationId, String applicationEnvironmentId,
            SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials) {
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, applicationEnvironmentId);
        Application application = applicationService.checkAndGetApplication(applicationId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);
        try {
            undeployService.undeployEnvironment(secretProviderConfigurationAndCredentials, applicationEnvironmentId);
        } catch (OrchestratorDisabledException e) {
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
    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/active-deployment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Deployment> getActiveDeployment(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment, ApplicationEnvironmentRole.APPLICATION_USER);
        Deployment deployment = deploymentService.getActiveDeployment(environment.getId());
        return RestResponseBuilder.<Deployment> builder().data(deployment).build();
    }

    /**
     * Get the active deployment monitoring data.
     *
     * @param applicationId id of the topology
     * @return the active deployment
     */
    @ApiOperation(value = "Get active deployment for the given application on the given cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/active-deployment-monitored", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<MonitoredDeploymentDTO> getActiveDeploymentMonitored(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment, ApplicationEnvironmentRole.APPLICATION_USER);
        Deployment deployment = deploymentService.getActiveDeployment(environment.getId());
        ApplicationTopologyVersion topologyVersion = applicationVersionService
                .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
        Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());

        MonitoredDeploymentDTO monitoredDeploymentDTO = new MonitoredDeploymentDTO();
        monitoredDeploymentDTO.setDeployment(deployment);
        Map<String, Integer> stepInstanceCount = toscaContextualAspect.execInToscaContext(() -> TopologyUtils.estimateWorkflowStepInstanceCount(topology), true, topology);
        monitoredDeploymentDTO.setWorkflowExpectedStepInstanceCount(stepInstanceCount);

        return RestResponseBuilder.<MonitoredDeploymentDTO> builder().data(monitoredDeploymentDTO).build();
    }

    private Map<String, Integer> countNodeInstance(Topology topology) {
        return TopologyUtils.estimateWorkflowStepInstanceCount(topology);
    }

    @ApiOperation(value = "Get current secret provider configuration for the given application on the given cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/current-secret-provider-configurations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<SecretCredentialInfo>> getSecretProviderConfigurationsForCurrentDeployment(@PathVariable String applicationId,
            @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment, ApplicationEnvironmentRole.APPLICATION_USER);
        Deployment deployment = deploymentService.getActiveDeployment(environment.getId());
        List<SecretCredentialInfo> secretProviderConfigurations = Lists.newArrayList();
        if (deployment != null) {
            for (int i = 0; i < deployment.getLocationIds().length; i++) {
                Location location = locationService.getOrFail(deployment.getLocationIds()[i]);
                if (location.getSecretProviderConfiguration() != null) {
                    secretProviderConfigurations.add(secretProviderService.getSecretCredentialInfo(location.getSecretProviderConfiguration().getPluginName(),
                            location.getSecretProviderConfiguration().getConfiguration()));
                }
            }
        }
        return RestResponseBuilder.<List<SecretCredentialInfo>> builder().data(secretProviderConfigurations).build();
    }

    @ApiOperation(value = "Update the active deployment for the given application on the given cloud.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/update-deployment", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public DeferredResult<RestResponse<Void>> updateDeployment(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @ApiParam(value = "The secret provider configuration and credentials.") @RequestBody SecretProviderCredentials secretProviderCredentials) {
        final DeferredResult<RestResponse<Void>> result = new DeferredResult<>(15L * 60L * 1000L);

        Application application = applicationService.checkAndGetApplication(applicationId);
        // get the topology from the version and the cloud from the environment
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        if (!environment.getApplicationId().equals(applicationId)) {
            throw new NotFoundException("Unable to find environment with id <" + applicationEnvironmentId + "> for application <" + applicationId + ">");
        }
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment, ApplicationEnvironmentRole.APPLICATION_USER);
        // check that the environment is not already deployed
        boolean isEnvironmentDeployed = applicationEnvironmentService.isDeployed(environment.getId());
        if (!isEnvironmentDeployed) {
            // the topology must be deployed in order to update it
            throw new NotFoundException(
                    "Application <" + applicationId + "> is not deployed for environment with id <" + applicationEnvironmentId + ">, can't update it");
        }

        Deployment deployment = deploymentService.getActiveDeployment(environment.getId());
        if (deployment == null) {
            throw new NotFoundException("Unable to find deployment for environment with id <" + applicationEnvironmentId + "> application <" + applicationId
                    + ">, can't update it");
        }

        ApplicationTopologyVersion topologyVersion = applicationVersionService
                .getOrFail(Csar.createId(environment.getApplicationId(), environment.getVersion()), environment.getTopologyVersion());
        Topology topology = topologyServiceCore.getOrFail(topologyVersion.getArchiveId());
        DeploymentTopologyDTO deploymentTopologyDTO = deploymentTopologyDTOBuilder.prepareDeployment(topology, application, environment);
        TopologyValidationResult validation = deploymentTopologyDTO.getValidation();

        deploymentService.checkDeploymentUpdateFeasibility(deployment, deploymentTopologyDTO.getTopology());

        // if not valid, then return validation errors
        if (!validation.isValid()) {
            result.setErrorResult(RestResponseBuilder.<Void> builder()
                    .error(new RestError(RestErrorCode.INVALID_DEPLOYMENT_TOPOLOGY.getCode(), "The deployment topology for the application <"
                            + application.getName() + "> on the environment <" + environment.getName() + "> is not valid."))
                    .build());
        }

        // process with the deployment
        deployService.update(secretProviderCredentials, deploymentTopologyDTO.getTopology(), application, deployment, new IPaaSCallback<Object>() {
            @Override
            public void onSuccess(Object data) {
                result.setResult(RestResponseBuilder.<Void> builder().build());
            }

            @Override
            public void onFailure(Throwable e) {
                result.setErrorResult(
                        RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.UNCATEGORIZED_ERROR.getCode(), e.getMessage())).build());
            }
        });

        return result;
    }

    /**
     * Get runtime topology of an application on a specific environment or the current deployment topology if no deployment is active.
     * This method is necessary for example to compute output properties / attributes on the client side.
     *
     * @param applicationId application id for which to get the topology
     * @param applicationEnvironmentId application environment for which to get the topology
     * @return {@link RestResponse}<{@link TopologyDTO}> containing the requested runtime {@link Topology} and the
     *         {@link NodeType} related to its {@link NodeTemplate}s
     */
    @ApiOperation(value = "Get last runtime (deployed) topology of an application or else get the current deployment topology for the environment.")
    @RequestMapping(value = "/{applicationId:.+?}/environments/{applicationEnvironmentId:.+?}/runtime-topology", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<TopologyDTO> getRuntimeTopology(
            @ApiParam(value = "Id of the application for which to get deployed topology.", required = true) @PathVariable String applicationId,
            @ApiParam(value = "Id of the environment for which to get deployed topology.", required = true) @PathVariable String applicationEnvironmentId) {
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, applicationEnvironmentId);
        if (!environment.getApplicationId().equals(applicationId)) {
            throw new NotFoundException("Unable to find environment with id <" + applicationEnvironmentId + "> for application <" + applicationId + ">");
        }
        AuthorizationUtil.checkAuthorizationForEnvironment(applicationService.getOrFail(applicationId), environment,
                ApplicationEnvironmentRole.APPLICATION_USER);
        Deployment deployment = deploymentService.getActiveDeployment(environment.getId());
        DeploymentTopology deploymentTopology = null;
        if (deployment != null) {
            deploymentTopology = deploymentRuntimeStateService.getRuntimeTopology(deployment.getId());
        }
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyDTOBuilder.initTopologyDTO(deploymentTopology, new TopologyDTO())).build();
    }

    @ApiOperation(value = "Deprecated Get the deployment status for the environements that the current user is allowed to see for a given application.", notes = "Returns the current status of an application list from the PaaS it is deployed on for all environments.")
    @RequestMapping(value = "/statuses", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Deprecated
    public RestResponse<Map<String, Map<String, EnvironmentStatusDTO>>> getApplicationsStatuses(@RequestBody List<String> applicationIds) {
        Map<String, Map<String, EnvironmentStatusDTO>> statuses = Maps.newHashMap();

        for (String applicationId : applicationIds) {
            Map<String, EnvironmentStatusDTO> environmentStatuses = Maps.newHashMap();
            Application application = applicationService.checkAndGetApplication(applicationId);
            // get all environments status for the current application
            ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
            for (ApplicationEnvironment env : environments) {
                if (AuthorizationUtil.hasAuthorizationForEnvironment(application, env, ApplicationEnvironmentRole.values())) {
                    DeploymentStatus status = DeploymentStatus.UNKNOWN;
                    try {
                        status = applicationEnvironmentService.getStatus(env);
                    } catch (Exception e) {
                        log.debug("Getting status for the environment <" + env.getId()
                                + "> failed because the associated orchestrator seems disabled. Returned status is UNKNOWN.", e);
                    }
                    // TODO: include environment roles in the DTO to help display on ui
                    environmentStatuses.put(env.getId(), new EnvironmentStatusDTO(env.getName(), status));
                }
            }
            statuses.put(applicationId, environmentStatuses);
        }
        return RestResponseBuilder.<Map<String, Map<String, EnvironmentStatusDTO>>> builder().data(statuses).build();
    }

    @ApiOperation(value = "Get all environments including their current deployment status for a list of applications.", notes = "Return the environements for all given applications. Note that only environments the user is authorized to see are returned.")
    @RequestMapping(value = "/environments", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Deprecated
    public RestResponse<Map<String, ApplicationEnvironmentDTO[]>> getApplicationsEnvironments(@RequestBody List<String> applicationIds) {
        Map<String, ApplicationEnvironmentDTO[]> envsByApplicationId = Maps.newHashMap();
        for (String applicationId : applicationIds) {
            Application application = applicationService.checkAndGetApplication(applicationId);
            // get all environments status for the current application
            ApplicationEnvironment[] environments = applicationEnvironmentService.getByApplicationId(application.getId());
            List<ApplicationEnvironmentDTO> userEnvironmentList = new ArrayList<>(environments.length);
            for (ApplicationEnvironment env : environments) {
                if (AuthorizationUtil.hasAuthorizationForEnvironment(application, env, ApplicationEnvironmentRole.values())) {
                    userEnvironmentList.add(dtoBuilder.getApplicationEnvironmentDTO(env));
                }
            }
            envsByApplicationId.put(applicationId, userEnvironmentList.toArray(new ApplicationEnvironmentDTO[userEnvironmentList.size()]));
        }
        return RestResponseBuilder.<Map<String, ApplicationEnvironmentDTO[]>> builder().data(envsByApplicationId).build();
    }

    /**
     * Get detailed information for every instances of every node of the application on the PaaS.
     *
     * @param applicationId The id of the application to be deployed.
     * @return A {@link RestResponse} that contains detailed informations (See {@link InstanceInformation}) of the application on the PaaS it is deployed.
     */
    @ApiOperation(value = "Get detailed informations for every instances of every node of the application on the PaaS.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ] and Application environment role required [ APPLICATION_USER | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/deployment/informations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public DeferredResult<RestResponse<Map<String, Map<String, InstanceInformation>>>> getInstanceInformation(@PathVariable String applicationId,
            @PathVariable String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment, ApplicationEnvironmentRole.values());

        Deployment deployment = applicationEnvironmentService.getActiveDeployment(environment.getId());
        final DeferredResult<RestResponse<Map<String, Map<String, InstanceInformation>>>> instancesDeferredResult = new DeferredResult<>(5L * 60L * 1000L);
        if (deployment == null) { // if there is no topology associated with the version it could not have been deployed.
            instancesDeferredResult.setResult(RestResponseBuilder.<Map<String, Map<String, InstanceInformation>>> builder().build());
        } else {
            try {
                deploymentRuntimeStateService.getInstancesInformation(deployment, new IPaaSCallback<Map<String, Map<String, InstanceInformation>>>() {
                    @Override
                    public void onSuccess(Map<String, Map<String, InstanceInformation>> data) {
                        instancesDeferredResult.setResult(RestResponseBuilder.<Map<String, Map<String, InstanceInformation>>> builder().data(data).build());
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        instancesDeferredResult.setErrorResult(throwable);
                    }
                });
            } catch (OrchestratorDisabledException e) {
                log.error("Cannot get instance informations as topology plugin cannot be found.", e);
                instancesDeferredResult.setResult(RestResponseBuilder.<Map<String, Map<String, InstanceInformation>>> builder().build());
            }
        }
        return instancesDeferredResult;
    }

    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/deployment/maintenance", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> switchMaintenanceModeOn(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);
        try {
            deploymentRuntimeService.switchMaintenanceMode(environment.getId(), true);
        } catch (OrchestratorDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        } catch (MaintenanceModeException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.MAINTENANCE_MODE_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/deployment/maintenance", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> switchMaintenanceModeOff(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId) {
        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);
        try {
            deploymentRuntimeService.switchMaintenanceMode(environment.getId(), false);
        } catch (OrchestratorDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        } catch (MaintenanceModeException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.MAINTENANCE_MODE_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/deployment/{nodeTemplateId}/{instanceId}/maintenance", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> switchInstanceMaintenanceModeOn(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @PathVariable String nodeTemplateId, @PathVariable String instanceId) {
        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);
        try {
            deploymentRuntimeService.switchInstanceMaintenanceMode(environment.getId(), nodeTemplateId, instanceId, true);
        } catch (OrchestratorDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        } catch (MaintenanceModeException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.MAINTENANCE_MODE_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/deployment/{nodeTemplateId}/{instanceId}/maintenance", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> switchInstanceMaintenanceModeOff(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @PathVariable String nodeTemplateId, @PathVariable String instanceId) {
        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);
        try {
            deploymentRuntimeService.switchInstanceMaintenanceMode(environment.getId(), nodeTemplateId, instanceId, false);
        } catch (OrchestratorDisabledException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build();
        } catch (MaintenanceModeException e) {
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.MAINTENANCE_MODE_ERROR.getCode(), e.getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    private ApplicationEnvironment getAppEnvironmentAndCheckAuthorization(String applicationId, String applicationEnvironmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(application.getId(), applicationEnvironmentId);
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);
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
    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/scale/{nodeTemplateId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit(bodyHiddenFields = { "credentials" })
    public DeferredResult<RestResponse<Void>> scale(@PathVariable String applicationId, @PathVariable String applicationEnvironmentId,
            @PathVariable String nodeTemplateId, @RequestParam int instances,
            @ApiParam(value = "The secret provider configuration ans credentials.") @RequestBody SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials) {
        final DeferredResult<RestResponse<Void>> result = new DeferredResult<>(15L * 60L * 1000L);
        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);

        try {
            deploymentRuntimeService.scale(secretProviderConfigurationAndCredentials, environment.getId(), nodeTemplateId, instances,
                    new IPaaSCallback<Object>() {
                        @Override
                        public void onSuccess(Object data) {
                            result.setResult(RestResponseBuilder.<Void> builder().build());
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            result.setErrorResult(
                                    RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.SCALING_ERROR.getCode(), e.getMessage())).build());
                        }
                    });
        } catch (OrchestratorDisabledException e) {
            result.setErrorResult(
                    RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build());
        } catch (PaaSDeploymentException e) {
            result.setErrorResult(RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.SCALING_ERROR.getCode(), e.getMessage())).build());
        }

        return result;
    }

    @ApiOperation(value = "Launch a given workflow.", authorizations = { @Authorization("ADMIN"), @Authorization("APPLICATION_MANAGER") })
    @RequestMapping(value = "/{applicationId:.+}/environments/{applicationEnvironmentId}/workflows/{workflowName}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit(bodyHiddenFields = { "credentials" })
    public DeferredResult<RestResponse<Void>> launchWorkflow(
            @ApiParam(value = "Application id.", required = true) @Valid @NotBlank @PathVariable String applicationId,
            @ApiParam(value = "Deployment id.", required = true) @Valid @NotBlank @PathVariable String applicationEnvironmentId,
            @ApiParam(value = "Workflow name.", required = true) @Valid @NotBlank @PathVariable String workflowName,
            @ApiParam(value = "The secret provider configuration and credentials.") @RequestBody(required = false) SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials) {

        final DeferredResult<RestResponse<Void>> result = new DeferredResult<>(15L * 60L * 1000L);
        ApplicationEnvironment environment = getAppEnvironmentAndCheckAuthorization(applicationId, applicationEnvironmentId);

        // TODO merge with incoming params
        Map<String, Object> params = Maps.newHashMap();

        try {
            workflowExecutionService.launchWorkflow(secretProviderConfigurationAndCredentials, environment.getId(), workflowName, params,
                    new IPaaSCallback<Object>() {
                        @Override
                        public void onSuccess(Object data) {
                            result.setResult(RestResponseBuilder.<Void> builder().build());
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            result.setErrorResult(RestResponseBuilder.<Void> builder()
                                    .error(new RestError(RestErrorCode.RUNTIME_WORKFLOW_ERROR.getCode(), e.getMessage())).build());
                        }
                    });
        } catch (OrchestratorDisabledException e) {
            result.setErrorResult(
                    RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage())).build());
        } catch (PaaSDeploymentException e) {
            result.setErrorResult(RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.SCALING_ERROR.getCode(), e.getMessage())).build());
        }

        return result;
    }

}
