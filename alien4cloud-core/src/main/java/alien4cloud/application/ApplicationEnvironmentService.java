package alien4cloud.application;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.events.OnDeploymentConfigCopyEvent;
import org.alien4cloud.alm.events.AfterApplicationEnvironmentDeleted;
import org.alien4cloud.alm.events.AfterEnvironmentTopologyVersionChanged;
import org.alien4cloud.alm.events.BeforeApplicationEnvironmentDeleted;
import org.alien4cloud.tosca.model.Csar;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.deployment.DeploymentLockService;
import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.deployment.OrchestratorPropertiesValidationService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.DeleteDeployedException;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationTopologyVersion;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.IResourceRoles;
import alien4cloud.security.model.ApplicationEnvironmentRole;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.Role;
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
    private ApplicationEventPublisher publisher;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private DeploymentLockService deploymentLockService;
    @Inject
    private OrchestratorPropertiesValidationService orchestratorPropertiesValidationService;

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
        Map<String, String[]> filters = MapUtil.newHashMap(new String[] { "applicationId" }, new String[][] { new String[] { applicationId } });
        return alienDAO.search(ApplicationEnvironment.class, null, filters, null, null, 0, Integer.MAX_VALUE, "name.lower_case", false).getData();
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
    public void delete(String id) {
        ApplicationEnvironment environment = getOrFail(id);
        deleteCheck(environment);
        deleteEnvironment(environment);
    }

    /**
     * Ensure that every versions of the application can be deleted, throw an exception if not.
     *
     * @param applicationId The id of the application to be deleted.
     */
    public DeleteApplicationEnvironments prepareDeleteByApplication(String applicationId) {
        ApplicationEnvironment[] environments = getByApplicationId(applicationId);
        for (ApplicationEnvironment environment : environments) {
            deleteCheck(environment);
        }
        return new DeleteApplicationEnvironments(environments);
    }

    /**
     * This object is returned by the prepareDeleteByApplication operation to ensure that we don't trigger a delete without having performed a check first.
     */
    public class DeleteApplicationEnvironments {
        private ApplicationEnvironment[] environments;

        DeleteApplicationEnvironments(ApplicationEnvironment[] environments) {
            this.environments = environments;
        }

        /**
         * Delete all versions related to an application.
         */
        public void delete() {
            for (ApplicationEnvironment environment : environments) {
                deleteEnvironment(environment);
            }
        }
    }

    private void deleteCheck(ApplicationEnvironment environment) {
        boolean isDeployed = isDeployed(environment.getId());

        if (isDeployed) {
            throw new DeleteDeployedException("Application environment with id <" + environment + "> cannot be deleted since it is deployed");
        }

        failIfExposedAsService(environment);
    }

    private void deleteEnvironment(ApplicationEnvironment environment) {
        publisher.publishEvent(new BeforeApplicationEnvironmentDeleted(this, environment.getApplicationId(), environment.getId()));
        alienDAO.delete(ApplicationEnvironment.class, environment.getId());
        publisher.publishEvent(new AfterApplicationEnvironmentDeleted(this, environment.getApplicationId(), environment.getId()));
    }

    /**
     * Get an active deployment associated with an environment.
     *
     * @param environmentId The id of the environment for which to get an active deployment.
     * @return The deployment associated with the environment.
     */
    public Deployment getActiveDeployment(String environmentId) {
        return alienDAO.buildQuery(Deployment.class).setFilters(fromKeyValueCouples("environmentId", environmentId, "endDate", null)).prepareSearch().find();
    }

    /**
     * True when an application environment is deployed
     * 
     * @return true if the environment is currently deployed
     */
    public boolean isDeployed(String environmentId) {
        return alienDAO.buildQuery(Deployment.class).setFilters(fromKeyValueCouples("environmentId", environmentId, "endDate", null)).count() > 0;
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
            log.debug("Application environment with name [ {} ] already exists for application id [ {} ]", name, applicationId);
            throw new AlreadyExistException("An application environment with the given name already exists");
        }
    }

    /**
     * Check rights on the related application and get the application environment
     * If no roles mentioned, all {@link ApplicationRole} values will be used
     * 
     * @param applicationEnvironmentId application's environment id
     * @param roles {@link ApplicationRole} to check right on the underlying application
     * @return the corresponding application environment
     */
    public ApplicationEnvironment checkAndGetApplicationEnvironment(String applicationEnvironmentId, IResourceRoles... roles) {
        ApplicationEnvironment applicationEnvironment = getOrFail(applicationEnvironmentId);
        // Does the user is allowed access (application, environment) level ?
        if(AuthorizationUtil.hasAuthorization(applicationEnvironment, Role.ADMIN, roles)){
            return applicationEnvironment;
        }
        // Does the user is allowed to access at the application level
        applicationService.checkAndGetApplication(applicationEnvironment.getApplicationId(), roles);
        return applicationEnvironment;
    }

    /**
     * Synchronize inputs between two environments
     * 
     * @param source the source environment
     * @param target the target environment
     */
    public void synchronizeEnvironmentInputs(ApplicationEnvironment source, ApplicationEnvironment target) {
        // We use an event to trigger copy of deployment configuration.
        // This allow easy addition for future environment specific topology modifiers configuration copy.
        // Within alien4cloud the event is processed with the following defined order
        // 10 inputs copy
        // 11 input artifacts copy
        // 20 location policies copy
        // 30 node matching and node configuration copy
        // 40 copy orchestrator specific properties
        publisher.publishEvent(new OnDeploymentConfigCopyEvent(this, source, target));
    }

    /**
     * Get the deployment status of the given environment.
     *
     * @param environment The environment for which to get deployment status.
     * @return The deployment status of the environment. {@link DeploymentStatus}.
     * @throws ExecutionException In case there is a failure while communicating with the orchestrator.
     * @throws InterruptedException In case there is a failure while communicating with the orchestrator.
     */
    public DeploymentStatus getStatus(ApplicationEnvironment environment) {
        final Deployment deployment = getActiveDeployment(environment.getId());
        return getStatus(deployment);
    }

    /**
     * Get the deployment status of the given deployment.
     *
     * @param deployment The deployment for which to get deployment status.
     * @return The deployment status of the environment. {@link DeploymentStatus}.
     * @throws ExecutionException In case there is a failure while communicating with the orchestrator.
     * @throws InterruptedException In case there is a failure while communicating with the orchestrator.
     */
    public DeploymentStatus getStatus(final Deployment deployment) {
        if (deployment == null) {
            return DeploymentStatus.UNDEPLOYED;
        }
        return deploymentLockService.doWithDeploymentReadLock(deployment.getOrchestratorDeploymentId(), () -> {
            DeploymentStatus currentStatus = deploymentRuntimeStateService.getDeploymentStatus(deployment);
            if (DeploymentStatus.UNDEPLOYED.equals(currentStatus)) {
                deploymentService.markUndeployed(deployment);
            }
            return currentStatus;
        });
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

    public void updateTopologyVersion(ApplicationEnvironment applicationEnvironment, String oldTopologyVersion, String newVersion, String newTopologyVersion,
            String environmentIdToCopyInput) {
        applicationEnvironment.setVersion(newVersion);
        applicationEnvironment.setTopologyVersion(newTopologyVersion);
        if (environmentIdToCopyInput != null) {
            ApplicationEnvironment environmentToCopyInput = checkAndGetApplicationEnvironment(environmentIdToCopyInput, ApplicationRole.APPLICATION_MANAGER);
            alienDAO.save(applicationEnvironment);
            synchronizeEnvironmentInputs(environmentToCopyInput, applicationEnvironment);
        } else {
            alienDAO.save(applicationEnvironment);
        }
        publisher.publishEvent(new AfterEnvironmentTopologyVersionChanged(this, oldTopologyVersion, newTopologyVersion, applicationEnvironment.getId(),
                applicationEnvironment.getApplicationId()));
    }

    private void failIfExposedAsService(ApplicationEnvironment environment) {
        if (alienDAO.buildQuery(ServiceResource.class).setFilters(fromKeyValueCouples("environmentId", environment.getId())).prepareSearch().count() > 0) {
            throw new DeleteReferencedObjectException("Environment " + environment.getApplicationId() + "/" + environment.getName() + "(" + environment.getId()
                    + ") could not be deleted since it is exposed as a service.");
        }
    }
}
