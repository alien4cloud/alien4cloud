package alien4cloud.paas.ha;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.AvailabilityZone;
import alien4cloud.paas.exception.ResourceMatchingFailedException;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSTopology;

import com.google.common.collect.Maps;

/**
 * This allocator helps to allocate an availability zone to a compute based on its HA policy
 */
@Slf4j
public class AvailabilityZoneAllocator {

    /**
     * Allocate availability zones to nodes which have HA policy in a topology
     * 
     * @param topology the parsed topology to consider
     * @param deploymentSetup the deployment setup
     * @return A map of compute node id --> Alien Availability Zone
     */
    public Map<String, AvailabilityZone> processAllocation(PaaSTopology topology, DeploymentSetup deploymentSetup) {
        Map<String, List<PaaSNodeTemplate>> groups = topology.getGroups();
        Map<String, AvailabilityZone> haComputeMap = Maps.newHashMap();
        if (groups != null) {
            for (Map.Entry<String, List<PaaSNodeTemplate>> groupEntry : groups.entrySet()) {
                if (deploymentSetup.getAvailabilityZoneMapping() == null) {
                    throw new ResourceMatchingFailedException("Need at least 1 availability zone configured to process allocation");
                }
                Collection<AvailabilityZone> availabilityZones = deploymentSetup.getAvailabilityZoneMapping().get(groupEntry.getKey());
                if (availabilityZones == null || availabilityZones.isEmpty()) {
                    throw new ResourceMatchingFailedException("Need at least 1 availability zone configured to process allocation");
                }
                Map<AvailabilityZone, Integer> availabilityZoneRepartition = Maps.newHashMap();
                for (AvailabilityZone availabilityZone : availabilityZones) {
                    availabilityZoneRepartition.put(availabilityZone, 0);
                }
                for (PaaSNodeTemplate compute : groupEntry.getValue()) {
                    if (haComputeMap.put(compute.getId(), getLeastUsedAvailabilityZone(availabilityZoneRepartition)) != null) {
                        log.error("Cannot manage this use case : [" + compute.getId()
                                + "] belongs to multiple HA group, only one availability zone will be selected");
                    }
                }
            }
        }
        return haComputeMap;
    }

    private AvailabilityZone getLeastUsedAvailabilityZone(Map<AvailabilityZone, Integer> availabilityZoneRepartition) {
        AvailabilityZone leastUsed = null;
        int leastUsedCount = 0;
        for (Map.Entry<AvailabilityZone, Integer> repartitionEntry : availabilityZoneRepartition.entrySet()) {
            if (leastUsed == null || leastUsedCount > repartitionEntry.getValue()) {
                leastUsed = repartitionEntry.getKey();
                leastUsedCount = repartitionEntry.getValue();
            }
        }
        availabilityZoneRepartition.put(leastUsed, leastUsedCount + 1);
        return leastUsed;
    }
}
