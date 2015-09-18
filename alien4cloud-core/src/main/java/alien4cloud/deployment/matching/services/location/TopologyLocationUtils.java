package alien4cloud.deployment.matching.services.location;

import org.apache.commons.collections4.MapUtils;

import alien4cloud.deployment.exceptions.LocationRequiredException;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.topology.AbstractPolicy;
import alien4cloud.model.topology.LocationPlacementPolicy;
import alien4cloud.model.topology.NodeGroup;

/**
 * Utility class to get Topology Location from a Deployment Topology.
 */
public final class TopologyLocationUtils {
    private TopologyLocationUtils() {
    }

    /**
     * Get the location on which a Deployment Topology should be deployed.
     *
     * @param deploymentTopology the deployment topology.
     */
    public static String getLocationIdOrFail(DeploymentTopology deploymentTopology) {
        String locationId = getLocationId(deploymentTopology);
        if (locationId != null) {
            return locationId;
        }
        throw new LocationRequiredException("No location placement policy has been found for the topology.");
    }

    /**
     * Get the location on which a Deployment Topology should be deployed.
     *
     * @param deploymentTopology the deployment topology.
     */
    public static String getLocationId(DeploymentTopology deploymentTopology) {
        if (MapUtils.isNotEmpty(deploymentTopology.getLocationGroups())) {
            for (NodeGroup group : deploymentTopology.getLocationGroups().values()) {
                for (AbstractPolicy policy : group.getPolicies()) {
                    if (policy instanceof LocationPlacementPolicy) {
                        return ((LocationPlacementPolicy) policy).getLocationId();
                    }
                }
            }
        }
        return null;
    }
}
