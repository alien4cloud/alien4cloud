package alien4cloud.application;

import javax.inject.Inject;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.topology.Topology;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;

/**
 * Service to manage deployment of application environments.
 */
public class ApplicationEnvironmentDeploymentService {
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationVersionService applicationVersionService;
    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;

    /**
     * Deploy the environment of a given application.
     * 
     * @param applicationId Id of the application the environment to deploy belongs to.
     * @param environmentId Id of the environment to deploy.
     */
    public void deploy(String applicationId, String environmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, environmentId);
        if (!environment.getApplicationId().equals(applicationId)) {
            throw new NotFoundException("Unable to find environment with id <" + environmentId + "> for application <" + applicationId + ">");
        }
        // Security check user must be authorized to deploy the environment (or be application manager)
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(environment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }

        // check that the environment is not already deployed
        boolean isEnvironmentDeployed = applicationEnvironmentService.isDeployed(environment.getId());
        if (isEnvironmentDeployed) {
            throw new AlreadyExistException("Environment with id <" + environmentId + "> for application <" + applicationId + "> is already deployed");
        }

        // Get the topology to be deployed.
        ApplicationVersion version = applicationVersionService.getVersionByIdOrDefault(environment.getApplicationId(), environment.getCurrentVersionId());
        Topology topology = topologyServiceCore.getOrFail(version.getTopologyId());

        // Get the deployment configurations (both location aware and matched topology and deployment properties)

        // process with the deployment

    }
}