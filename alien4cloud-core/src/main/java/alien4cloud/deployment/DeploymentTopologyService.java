package alien4cloud.deployment;

import javax.annotation.Resource;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.deployment.DeploymentTopology;
import org.springframework.stereotype.Service;

/**
 * Manages the deployment topology handling.
 */
@Service
public class DeploymentTopologyService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Get the deployment topology for a given version and environment.
     * 
     * @param versionId The id of the version for which to get the deployment topology.
     * @param environmentId The id of the environment for which to get the deployment topology.
     * @return The deployment topology for the given version and environment.
     */
    public DeploymentTopology getOrFail(String versionId, String environmentId) {
        DeploymentTopology deploymentTopology = alienDAO.findById(DeploymentTopology.class, generateId(versionId, environmentId));
        if (deploymentTopology == null) {
            throw new NotFoundException("Unable to find the deployment topology for version <" + versionId + "> and environment <" + environmentId + ">");
        }
        return deploymentTopology;
    }

    /**
     * Generate the id of a deployment setup.
     *
     * @param versionId The id of the version of the deployment setup.
     * @param environmentId The id of the environment of the deployment setup.
     * @return The generated id.
     */
    private String generateId(String versionId, String environmentId) {
        return versionId + "::" + environmentId;
    }
}
