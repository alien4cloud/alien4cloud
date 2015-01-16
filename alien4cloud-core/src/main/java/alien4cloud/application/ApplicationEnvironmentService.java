package alien4cloud.application;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.cloud.CloudService;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.security.ApplicationEnvironmentRole;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Slf4j
@Service
public class ApplicationEnvironmentService {

    private final static String DEFAULT_ENVIRONMENT_NAME = "Environment";

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private DeploymentSetupService deploymentSetupService;
    @Resource
    private DeploymentService deploymentService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private CloudService cloudService;

    /**
     * Method used to create a default environment
     *
     * @param applicationId The id of the application for which to create the environment.
     * @return The id of the newly created environment.
     */
    public ApplicationEnvironment createApplicationEnvironment(String user, String applicationId, String versionId) {
        return createApplicationEnvironment(user, applicationId, DEFAULT_ENVIRONMENT_NAME, null, EnvironmentType.OTHER, versionId);
    }

    /**
     * Create a new environment for a given application
     *
     * @param applicationId The id of the application.
     * @param name The environment name.
     * @param description The environment description.
     * @param environmentType The type of environment.
     * @return The newly created environment.
     */
    public ApplicationEnvironment createApplicationEnvironment(String user, String applicationId, String name, String description,
            EnvironmentType environmentType, String versionId) {
        // unique app env name for a given app
        ensureNameUnicity(applicationId, name);
        ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment();
        applicationEnvironment.setId(UUID.randomUUID().toString());
        applicationEnvironment.setName(name);
        applicationEnvironment.setDescription(description);
        applicationEnvironment.setEnvironmentType(environmentType);
        applicationEnvironment.setApplicationId(applicationId);
        applicationEnvironment.setCurrentVersionId(versionId);
        Map<String, Set<String>> userRoles = Maps.newHashMap();
        userRoles.put(user, Sets.newHashSet(ApplicationEnvironmentRole.DEPLOYMENT_MANAGER.toString()));
        applicationEnvironment.setUserRoles(userRoles);
        alienDAO.save(applicationEnvironment);
        return applicationEnvironment;
    }

    /**
     * Get all environments for a given application
     *
     * @param applicationId The id of the application for which to get environments.
     * @return An array of the environments for the requested application id.
     */
    public ApplicationEnvironment[] getByApplicationId(String applicationId) {
        GetMultipleDataResult<ApplicationEnvironment> result = alienDAO.find(ApplicationEnvironment.class,
                MapUtil.newHashMap(new String[] { "applicationId" }, new String[][] { new String[] { applicationId } }), Integer.MAX_VALUE);
        return result.getData();
    }

    /**
     * Delete a version and the related topologies.
     *
     * @param id The id of the version to delete.
     */
    public boolean delete(String id) {
        deploymentSetupService.deleteByEnvironmentId(id);
        // TODO : do not delete if it's last environment
        alienDAO.delete(ApplicationEnvironment.class, id);
        return true;
    }

    /**
     * Delete all environments related to an application
     *
     * @param applicationId The application id
     * @throws CloudDisabledException
     */
    public void deleteByApplication(String applicationId) throws CloudDisabledException {
        List<String> deployedEnvironments = Lists.newArrayList();
        ApplicationEnvironment[] environments = getByApplicationId(applicationId);
        for (ApplicationEnvironment environment : environments) {
            if (!this.isDeployed(environment.getId())) {
                delete(environment.getId());
            } else {
                // collect all deployed environment
                deployedEnvironments.add(environment.getId());
            }
        }
        // couln't delete deployed environment
        if (deployedEnvironments.size() > 0) {
            // error could not deployed all app environment for this applcation
            log.error("Cannot delete these deployed environments : {}", deployedEnvironments.toString());
        }
    }

    /**
     * Get an active deployment associated with an environment.
     *
     * @param appEnvironmentId The id of the environment for which to get an active deployment.
     * @return The deployment associated with the environment.
     */
    public Deployment getActiveDeployment(String appEnvironmentId) {
        GetMultipleDataResult<Deployment> dataResult = alienDAO.search(
                Deployment.class,
                null,
                MapUtil.newHashMap(new String[] { "deploymentSetup.environmentId", "endDate" }, new String[][] { new String[] { appEnvironmentId },
                        new String[] { null } }), 1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            return dataResult.getData()[0];
        }
        return null;
    }

    /**
     * True when an application environment is deployed
     * 
     * @return true if the environment is currently deployed
     */
    public boolean isDeployed(String appEnvironmentId) {
        if (getActiveDeployment(appEnvironmentId) == null) {
            return false;
        }
        return true;
    }

    /**
     * Get an application environment from it's id and throw a {@link NotFoundException} in case no application environment matches the requested id
     * 
     * @param applicationEnvironmentId
     * @return The requested application environment
     */
    public ApplicationEnvironment getOrFail(String applicationEnvironmentId) {
        ApplicationEnvironment applicationEnvironment = alienDAO.findById(ApplicationEnvironment.class, applicationEnvironmentId);
        if (applicationEnvironment == null) {
            throw new NotFoundException("Application environment [" + applicationEnvironmentId + "] cannot be found");
        }
        return applicationEnvironment;
    }

    /**
     * Check the the name of the application environment is already used
     *
     * @param name The name of the application environment
     * @return true if an application environment already use this name, false if not
     */
    public void ensureNameUnicity(String applicationId, String name) {
        long result = alienDAO.count(ApplicationEnvironment.class, null,
                MapUtil.newHashMap(new String[] { "applicationId", "name" }, new String[][] { new String[] { applicationId }, new String[] { name } }));
        if (result > 0) {
            log.debug("Application environment with name <{}> already exists for application id <{}>", name, applicationId);
            throw new AlreadyExistException("An application environment with the given name already exists");
        }
    }

    /**
     * Check rights on the related application and get the application environment
     * If no roles mentioned, all {@link ApplicationRole} values will be used
     * 
     * @param applicationEnvironmentId
     * @param roles {@link ApplicationRole} to check right on the underlying application
     * @return the corresponding application environment
     */
    public ApplicationEnvironment checkAndGetApplicationEnvironment(String applicationEnvironmentId, ApplicationRole... roles) {
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        Application application = applicationService.checkAndGetApplication(applicationEnvironment.getApplicationId());
        roles = (roles == null || roles.length == 0) ? ApplicationRole.values() : roles;
        // check rights on the application linked to this application environment
        AuthorizationUtil.checkAuthorizationForApplication(application, roles);
        return applicationEnvironment;
    }

    /**
     * Get the environment status regarding the linked topology and cloud
     * 
     * @param environment to determine the status
     * @return {@link DeploymentStatus}
     * @throws CloudDisabledException
     */
    public DeploymentStatus getStatus(ApplicationEnvironment environment) throws CloudDisabledException {
        final Deployment deployment = getActiveDeployment(environment.getId());
        if(deployment == null) {
            return DeploymentStatus.UNDEPLOYED;
        }
        if(deployment.getDeploymentStatus() == null) {
            if(deployment.getEndDate() == null) {
                return DeploymentStatus.UNDEPLOYED;
            }
            // update the deployment status from PaaS if it cannot be found.
            deploymentService.getDeploymentStatus(deployment, new IPaaSCallback<DeploymentStatus>() {
                @Override
                public void onSuccess(DeploymentStatus data) {
                    deployment.setDeploymentStatus(data);
                    alienDAO.save(deployment);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.error("Failed to request deployment status from PaaS for deployment <"+deployment.getId()+">", throwable);
                }
            });
            // and return unknown for now...
            return DeploymentStatus.UNKNOWN;
        }
        return deployment.getDeploymentStatus();
    }

    /**
     * Get the topology id linked to the environment
     * 
     * @param applicationEnvironmentId The id of the environment.
     * @return a topology id or null
     */
    public String getTopologyId(String applicationEnvironmentId) {
        ApplicationEnvironment applicationEnvironment = getOrFail(applicationEnvironmentId);
        ApplicationVersion applicationVersion = applicationVersionService.get(applicationEnvironment.getCurrentVersionId());
        return applicationVersion == null ? null : applicationVersion.getTopologyId();
    }
}
