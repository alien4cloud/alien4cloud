package alien4cloud.deployment;

import java.util.List;
import java.util.Map;

import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.plugin.aop.Overridable;

public interface IDeploymentNodeSubstitutionService {

    /**
     * Get all available substitutions for a processed deployment topology
     *
     * @param deploymentTopology
     * @return
     */
    @Overridable
    Map<String, List<LocationResourceTemplate>> getAvailableSubstitutions(DeploymentTopology deploymentTopology);

    /**
     * This method updates the node substitution choices and default selections for a given deployment topology.
     *
     * @param deploymentTopology The deployment topology in which to save substitutions / deploymentTopology.getNodeTemplates() are the nodes from the original
     *            topology.
     * @param nodesToMergeProperties The node that where substituted previously with specific configurations from deployment user.
     */
    void processNodesSubstitution(DeploymentTopology deploymentTopology, Map<String, NodeTemplate> nodesToMergeProperties);

}