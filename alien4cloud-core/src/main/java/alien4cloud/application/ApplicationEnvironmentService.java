package alien4cloud.application;

import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;

@Slf4j
@Service
public class ApplicationEnvironmentService {
    private final static String DEFAULT_ENVIRONMENT_NAME = "Environment";

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private DeploymentSetupService deploymentSetupService;

    /**
     * Method used to create a default environment
     *
     * @param applicationId The id of the application for which to create the environment.
     * @return The id of the newly created environment.
     */
    public ApplicationEnvironment createApplicationEnvironment(String applicationId) {
        return createApplicationEnvironment(applicationId, DEFAULT_ENVIRONMENT_NAME, null, EnvironmentType.OTHER);
    }

    /**
     * Create a new environment for a given application.
     *
     * @param applicationId The id of the application.
     * @param name The environment name.
     * @param description The environment description.
     * @param environmentType The type of environment.
     * @return The newly created environment.
     */
    public ApplicationEnvironment createApplicationEnvironment(String applicationId, String name, String description, EnvironmentType environmentType) {
        ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment();
        applicationEnvironment.setId(UUID.randomUUID().toString());
        applicationEnvironment.setName(name);
        applicationEnvironment.setDescription(description);
        applicationEnvironment.setEnvironmentType(environmentType);
        applicationEnvironment.setApplicationId(applicationId);
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
        alienDAO.delete(ApplicationEnvironment.class, id);
        return true;
    }

    /**
     * Delete all environments related to an application
     *
     * @param applicationId The application id
     */
    public void deleteByApplication(String applicationId) {
        // TODO check if the environment is deployed.
        List<String> deployedEnvironments = Lists.newArrayList();
        ApplicationEnvironment[] environments = getByApplicationId(applicationId);
        for (ApplicationEnvironment environment : environments) {
            if (!this.isDeployed(environment)) {
                delete(environment.getId());
            } else {
                // collect all deployed environment
                deployedEnvironments.add(environment.getId());
            }
        }

        // couln't delete deployed environment
        if (deployedEnvironments.size() > 0) {
            // error could not deployed all app environment fo this applcation
            log.error("Cannot delete these deployed environments : {}", deployedEnvironments.toString());
        }
    }

    /**
     * @return true if the environment is currently deployed
     */
    public boolean isDeployed(ApplicationEnvironment applicationEnvironment) {

        // First phase : there is at least one deploymentSetup with this applicationEnvironmentId
        GetMultipleDataResult<DeploymentSetup> deploymentSetupSearch = alienDAO.find(DeploymentSetup.class,
                MapUtil.newHashMap(new String[] { "environmentId" }, new String[][] { new String[] { applicationEnvironment.getId() } }), Integer.MAX_VALUE);

        // no deploymentSetup => no app environment deployed
        if (deploymentSetupSearch.getData().length == 0) {
            return false;
        }
        // Second phase : this deploymentSetup has a deployment in status DeploymentStatus.DEPLOYED
        for (DeploymentSetup deploymentSetup : deploymentSetupSearch.getData()) {
            // TODO get deployments for this deployment setup
        }
        return false;
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
}
