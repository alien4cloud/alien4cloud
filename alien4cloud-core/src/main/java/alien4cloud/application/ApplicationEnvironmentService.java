package alien4cloud.application;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.Csar;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.deployment.DeploymentTopologyService;
import alien4cloud.events.DeleteEnvironmentEvent;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.DeleteDeployedException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationTopologyVersion;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ApplicationEnvironmentService {
    private final static String DEFAULT_ENVIRONMENT_NAME = "Environment";

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationVersionService applicationVersionService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;
    @Resource
    private ApplicationContext applicationContext;
    @Inject
    private DeploymentService deploymentService;

    /**
     * Method used to create a default environment
     *
     * @param applicationId The id of the application for which to create the environment.
     * @return The id of the newly created environment.
     */
    public ApplicationEnvironment createApplicationEnvironment(String user, String applicationId, String topologyVersion) {
        return createApplicationEnvironment(user, applicationId, DEFAULT_ENVIRONMENT_NAME, null, EnvironmentType.OTHER, topologyVersion);
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
            EnvironmentType environmentType, String topologyVersion) {
        ApplicationVersion applicationVersion = applicationVersionService.getOrFailByArchiveId(Csar.createId(applicationId, topologyVersion));
        if (!applicationVersion.getApplicationId().equals(applicationId)) {
            throw new IllegalArgumentException(
                    "The topology version with id <" + topologyVersion + "> is not a topology of a version of the application with id <" + applicationId + ">");
        }
        // unique app env name for a given app
        ensureNameUnicity(applicationId, name);
        ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment();
        applicationEnvironment.setId(UUID.randomUUID().toString());
        applicationEnvironment.setName(name);
        applicationEnvironment.setDescription(description);
        applicationEnvironment.setEnvironmentType(environmentType);
        applicationEnvironment.setApplicationId(applicationId);
        applicationEnvironment.setVersion(applicationVersion.getVersion());
        applicationEnvironment.setTopologyVersion(topologyVersion);
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
     * Get all environments for a given application
     *
     * @param versionId The id of the application for which to get environments.
     * @return An array of the environments for the requested application id.
     */
    public ApplicationEnvironment[] getByVersionId(String versionId) {
        GetMultipleDataResult<ApplicationEnvironment> result = alienDAO.find(ApplicationEnvironment.class,
                MapUtil.newHashMap(new String[] { "currentVersionId" }, new String[][] { new String[] { versionId } }), Integer.MAX_VALUE);
        return result.getData();
    }

    /**
     * Delete a version and the related topologies.
     *
     * @param id The id of the version to delete.
     */
    public boolean delete(String id) {
        ApplicationEnvironment environmentToDelete = getOrFail(id);
        boolean isDeployed = isDeployed(id);

        if (isDeployed) {
            throw new DeleteDeployedException("Application environment with id <" + id + "> cannot be deleted since it is deployed");
        }

        deploymentTopologyService.deleteByEnvironmentId(id);
        alienDAO.delete(ApplicationEnvironment.class, id);

        applicationContext
                .publishEvent(new DeleteEnvironmentEvent(this, environmentToDelete, deploymentService.getAllOrchestratorIdsAndOrchestratorDeploymentId(id)));
        return true;
    }

    /**
     * Delete all environments related to an application
     *
     * @param applicationId The application id
     */
    public void deleteByApplication(String applicationId) {
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
        if (!deployedEnvironments.isEmpty()) {
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
        GetMultipleDataResult<Deployment> dataResult = alienDAO.search(Deployment.class, null,
                MapUtil.newHashMap(new String[] { "environmentId", "endDate" }, new String[][] { new String[] { appEnvironmentId }, new String[] { null } }),
                1);
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
        ApplicationEnvironment applicationEnvironment = getOrFail(applicationEnvironmentId);
        applicationService.checkAndGetApplication(applicationEnvironment.getApplicationId(), roles);
        return applicationEnvironment;
    }

    /**
     * Get the environment status regarding the linked topology and cloud
     * 
     * @param environment The environment for which to get deployment status.
     * @return The deployment status of the environment. {@link DeploymentStatus}.
     * @throws ExecutionException In case there is a failure while communicating with the orchestrator.
     * @throws InterruptedException In case there is a failure while communicating with the orchestrator.
     */
    public DeploymentStatus getStatus(ApplicationEnvironment environment) throws ExecutionException, InterruptedException {
        final Deployment deployment = getActiveDeployment(environment.getId());
        return getStatus(deployment);
    }

    /**
     * Get the status of the given deployment.
     * 
     * @param deployment The deployment for which to get status.
     * @return The deployment status of the environment. {@link DeploymentStatus}.
     * @throws ExecutionException In case there is a failure while communicating with the orchestrator.
     * @throws InterruptedException In case there is a failure while communicating with the orchestrator.
     */
    public DeploymentStatus getStatus(final Deployment deployment) throws ExecutionException, InterruptedException {
        if (deployment == null) {
            return DeploymentStatus.UNDEPLOYED;
        }
        final SettableFuture<DeploymentStatus> statusSettableFuture = SettableFuture.create();
        // update the deployment status from PaaS if it cannot be found.
        deploymentRuntimeStateService.getDeploymentStatus(deployment, new IPaaSCallback<DeploymentStatus>() {
            @Override
            public void onSuccess(DeploymentStatus data) {
                statusSettableFuture.set(data);
            }

            @Override
            public void onFailure(Throwable throwable) {
                statusSettableFuture.setException(throwable);
            }
        });
        DeploymentStatus currentStatus = statusSettableFuture.get();
        if (DeploymentStatus.UNDEPLOYED.equals(currentStatus)) {
            deploymentService.markUndeployed(deployment);
        }
        return currentStatus;
    }

    /**
     * Get the topology id linked to the environment
     * 
     * @param applicationEnvironmentId The id of the environment.
     * @return a topology id or null
     */
    @Deprecated
    public String getTopologyId(String applicationEnvironmentId) {
        ApplicationEnvironment applicationEnvironment = getOrFail(applicationEnvironmentId);
        ApplicationVersion applicationVersion = applicationVersionService
                .getOrFailByArchiveId(Csar.createId(applicationEnvironment.getApplicationId(), applicationEnvironment.getTopologyVersion()));
        ApplicationTopologyVersion topologyVersion = applicationVersion == null ? null
                : applicationVersion.getTopologyVersions().get(applicationEnvironment.getTopologyVersion());
        return topologyVersion == null ? null : topologyVersion.getArchiveId();
    }

    /**
     * Get a environment for and application
     * 
     * @param applicationId
     * @param applicationEnvironmentId
     * @return
     */
    public ApplicationEnvironment getEnvironmentByIdOrDefault(String applicationId, String applicationEnvironmentId) {
        ApplicationEnvironment environment = null;
        if (applicationEnvironmentId == null) {
            ApplicationEnvironment[] applicationEnvironments = getByApplicationId(applicationId);
            environment = applicationEnvironments[0];
        } else {
            environment = getOrFail(applicationEnvironmentId);
        }
        return environment;
    }
}
