package alien4cloud.deployment.matching.services.location;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import alien4cloud.deployment.exceptions.LocationRequiredException;
import alien4cloud.model.deployment.DeploymentTopology;
import org.alien4cloud.tosca.model.templates.AbstractPolicy;
import org.alien4cloud.tosca.model.templates.LocationPlacementPolicy;
import org.alien4cloud.tosca.model.templates.NodeGroup;

import com.google.common.collect.Maps;

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
     * @return map of location group to location id
     */
    public static Map<String, String> getLocationIdsOrFail(DeploymentTopology deploymentTopology) {
        Map<String, String> locationIds = getLocationIds(deploymentTopology);
        if (MapUtils.isNotEmpty(locationIds)) {
            return locationIds;
        }
        throw new LocationRequiredException("No location placement policy has been found for the topology.");
    }

    /**
     * Get the location on which a Deployment Topology should be deployed.
     *
     * @param deploymentTopology the deployment topology.
     * @return map of location group to location id
     */
    public static Map<String, String> getLocationIds(DeploymentTopology deploymentTopology) {
        Map<String, String> locationIds = Maps.newHashMap();
        if (MapUtils.isNotEmpty(deploymentTopology.getLocationGroups())) {
            for (NodeGroup group : deploymentTopology.getLocationGroups().values()) {
                for (AbstractPolicy policy : group.getPolicies()) {
                    if (policy instanceof LocationPlacementPolicy) {
                        locationIds.put(group.getName(), ((LocationPlacementPolicy) policy).getLocationId());
                    }
                }
            }
        }
        return locationIds;
    }
}
