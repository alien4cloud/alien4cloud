package alien4cloud.deployment;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.application.TopologyCompositionService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.deployment.DeploymentSetup;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.topology.Topology;
import alien4cloud.utils.services.ConstraintPropertyService;

/**
 * Manages the deployment topology handling.
 */
@Service
public class DeploymentTopologyService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ConstraintPropertyService constraintPropertyService;
    @Resource
    private InputsPreProcessorService inputsPreProcessorService;
    @Resource
    private TopologyCompositionService topologyCompositionService;
    @Resource
    private DeploymentSetupService deploymentSetupService;

    /**
     * Get the deployment topology for a given version and environment.
     * 
     * @param versionId The id of the version for which to get the deployment topology.
     * @param environmentId The id of the environment for which to get the deployment topology.
     * @return The deployment topology for the given version and environment.
     */
    public DeploymentTopology getDeployedTopology(String versionId, String environmentId) {
        DeploymentTopology deploymentTopology = alienDAO.findById(DeploymentTopology.class, generateId(versionId, environmentId));
        if (deploymentTopology == null) {
            throw new NotFoundException("Unable to find the deployment topology for version <" + versionId + "> and environment <" + environmentId + ">");
        }
        return deploymentTopology;
    }

    public DeploymentTopology generateDeploymentTopology(Topology topology, ApplicationEnvironment environment, ApplicationVersion version) {
        DeploymentSetup deploymentSetup = deploymentSetupService.get(version, environment);
        if (deploymentSetup == null) {
            deploymentSetup = deploymentSetupService.createOrFail(version, environment);
        }
        topologyCompositionService.processTopologyComposition(topology);
        // TODO generate the deployment topology
        return null;
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
