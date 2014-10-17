package alien4cloud.application;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.DeploymentSetup;

/**
 * Manages deployment setups.
 */
@Service
public class DeploymentSetupService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    public DeploymentSetup get(String versionId, String environmentId) {
        return alienDAO.findById(DeploymentSetup.class, generateId(versionId, environmentId));
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
