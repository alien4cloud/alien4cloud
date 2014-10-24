package alien4cloud.application;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.DeploymentSetup;

/**
 * Manages deployment setups.
 */
@Service
public class DeploymentSetupService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    private DeploymentSetup get(String versionId, String environmentId) {
        return alienDAO.findById(DeploymentSetup.class, generateId(versionId, environmentId));
    }

    public DeploymentSetup getOrFail(String versionId, String environmentId) {
        DeploymentSetup setup = get(versionId, environmentId);
        if (setup == null) {
            throw new NotFoundException("No setup found for version [" + versionId + "] and environment [" + environmentId + "]");
        } else {
            return setup;
        }
    }

    public DeploymentSetup create(ApplicationVersion version, ApplicationEnvironment environment) {
        DeploymentSetup deploymentSetup = new DeploymentSetup();
        deploymentSetup.setId(generateId(version.getId(), environment.getId()));
        deploymentSetup.setEnvironmentId(environment.getId());
        deploymentSetup.setVersionId(version.getId());
        alienDAO.save(deploymentSetup);
        return deploymentSetup;
    }

    public String generateId(String versionId, String environmentId) {
        return versionId + "::" + environmentId;
    }

    /**
     * Delete a deployment setup based on the id of the related environment.
     *
     * @param environmentId The id of the environment
     */
    public void deleteByEnvironmentId(String environmentId) {
        alienDAO.delete(DeploymentSetup.class, QueryBuilders.termQuery("environmentId", environmentId));
    }

    /**
     * Delete a deployment setup based on the id of the related version.
     *
     * @param environmentId The id of the version
     */
    public void deleteByVersionId(String environmentId) {
        alienDAO.delete(DeploymentSetup.class, QueryBuilders.termQuery("versionId", environmentId));
    }
}
