package alien4cloud.application;

import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.utils.MapUtil;

@Service
public class ApplicationEnvironmentService {
    private final static String DEFAULT_ENVIRONMENT_NAME = "Environment";

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private DeploymentSetupService deploymentSetupService;

    /**
     * Method used to create a default environment.
     *
     * @param applicationId The id of the application for which to create the environment.
     * @return The id of the newly created environment.
     */
    public String createApplicationEnvironment(String applicationId) {
        return createApplicationEnvironment(applicationId, DEFAULT_ENVIRONMENT_NAME, null, EnvironmentType.OTHER);
    }

    /**
     * Create a new environment for a given application.
     *
     * @param applicationId The id of the application.
     * @param name The environment name.
     * @param description The environment description.
     * @param environmentType The type of environment.
     * @return The id of the newly created environment.
     */
    public String createApplicationEnvironment(String applicationId, String name, String description, EnvironmentType environmentType) {
        ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment();
        applicationEnvironment.setId(UUID.randomUUID().toString());
        applicationEnvironment.setName(name);
        applicationEnvironment.setName(description);
        applicationEnvironment.setEnvironmentType(environmentType);
        applicationEnvironment.setApplicationId(applicationId);
        alienDAO.save(applicationEnvironment);
        return applicationEnvironment.getId();
    }

    /**
     * Get all environments for a given application.
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
    public void delete(String id) {
        deploymentSetupService.deleteByEnvironmentId(id);
        alienDAO.delete(ApplicationEnvironment.class, id);
    }

    /**
     * Delete all versions related to an application.
     *
     * @param applicationId The application id.
     */
    public void deleteByApplication(String applicationId) {
        // TODO check if the environment is deployed.
        ApplicationEnvironment[] environments = getByApplicationId(applicationId);
        for (ApplicationEnvironment environment : environments) {
            delete(environment.getId());
        }
    }

    /**
     *
     * @return true if the environment is currently deployed.
     */
    public boolean isDeployed() {
        return false;
    }
}
