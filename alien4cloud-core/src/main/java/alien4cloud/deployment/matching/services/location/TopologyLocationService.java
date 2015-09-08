package alien4cloud.deployment.matching.services.location;

import alien4cloud.deployment.exceptions.LocationRequiredException;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.topology.AbstractPolicy;
import alien4cloud.model.topology.LocationPlacementPolicy;
import alien4cloud.model.topology.NodeGroup;

/**
 * Created by luc on 08/09/2015.
 */
public class TopologyLocationService {

    /**
     * Get the location on which a Deployment Topology should be deployed.
     *
     * @param deploymentTopology the deployment topology.
     */
    public String getLocationId(DeploymentTopology deploymentTopology) {
        for (NodeGroup group : deploymentTopology.getGroups().values()) {
            for (AbstractPolicy policy : group.getPolicies()) {
                if (policy instanceof LocationPlacementPolicy) {
                    return ((LocationPlacementPolicy) policy).getLocationId();
                }
            }
        }
        throw new LocationRequiredException("No location placement policy has been found for the topology.");
    }
}
