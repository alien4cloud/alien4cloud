package alien4cloud.deployment;

import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.Maps;

import org.springframework.stereotype.Service;

import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the deployment topology handling.
 */
@Service
@Slf4j
public class DeploymentTopologyService {
    @Inject
    private LocationService locationService;

    /**
     * Get location map from the deployment topology
     *
     * @param deploymentTopology the deploymentTopology
     * @return map of location group id to location
     */
    public Map<String, Location> getLocations(DeploymentTopology deploymentTopology) {
        Map<String, String> locationIds = TopologyLocationUtils.getLocationIdsOrFail(deploymentTopology);
        return getLocations(locationIds);
    }

    /**
     * Get location map from the deployment topology
     *
     * @param locationIds map of group id to location id
     * @return map of location group id to location
     */
    public Map<String, Location> getLocations(Map<String, String> locationIds) {
        Map<String, Location> locations = locationService.getMultiple(locationIds.values());
        Map<String, Location> locationMap = Maps.newHashMap();
        for (Map.Entry<String, String> locationIdsEntry : locationIds.entrySet()) {
            locationMap.put(locationIdsEntry.getKey(), locations.get(locationIdsEntry.getValue()));
        }
        if (locations.size() < locationIds.values().stream().distinct().count()) {
            throw new NotFoundException("Some locations could not be found " + locationIds);
        }
        return locationMap;
    }
}
