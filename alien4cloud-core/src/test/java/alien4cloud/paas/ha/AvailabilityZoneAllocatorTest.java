package alien4cloud.paas.ha;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.AvailabilityZone;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSTopology;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Slf4j
public class AvailabilityZoneAllocatorTest {

    public static final String HA_GROUP = "HA_group";

    @Test
    public void test() {
        PaaSNodeTemplate compute1 = new PaaSNodeTemplate("Compute1", null);
        PaaSNodeTemplate compute2 = new PaaSNodeTemplate("Compute2", null);
        PaaSNodeTemplate compute3 = new PaaSNodeTemplate("Compute3", null);
        PaaSNodeTemplate compute4 = new PaaSNodeTemplate("Compute4", null);
        PaaSNodeTemplate compute5 = new PaaSNodeTemplate("Compute5", null);
        List<PaaSNodeTemplate> templates = Lists.newArrayList(compute1, compute2, compute3, compute4, compute5);
        Map<String, List<PaaSNodeTemplate>> groups = Maps.newHashMap();
        groups.put(HA_GROUP, templates);
        PaaSTopology topology = new PaaSTopology();
        topology.setGroups(groups);
        DeploymentSetup deploymentSetup = new DeploymentSetup();
        Map<String, Set<AvailabilityZone>> availabilityZoneMapping = Maps.newHashMap();
        Set<AvailabilityZone> availableZones = Sets.newHashSet();
        availableZones.add(new AvailabilityZone("zone1", "Zone 1"));
        availableZones.add(new AvailabilityZone("zone2", "Zone 2"));
        availableZones.add(new AvailabilityZone("zone3", "Zone 3"));
        availabilityZoneMapping.put(HA_GROUP, availableZones);
        deploymentSetup.setAvailabilityZoneMapping(availabilityZoneMapping);
        AvailabilityZoneAllocator allocator = new AvailabilityZoneAllocator();
        Map<String, AvailabilityZone> allocated = allocator.processAllocation(topology, deploymentSetup);

        Map<AvailabilityZone, Integer> availabilityZoneRepartition = Maps.newHashMap();
        for (AvailabilityZone availabilityZone : availableZones) {
            availabilityZoneRepartition.put(availabilityZone, 0);
        }

        for (Map.Entry<String, AvailabilityZone> allocatedEntry : allocated.entrySet()) {
            int usedCount = availabilityZoneRepartition.get(allocatedEntry.getValue());
            availabilityZoneRepartition.put(allocatedEntry.getValue(), usedCount + 1);
        }

        // The goal is to prove that with 3 availability zones, when 1 crash
        // We'll have computes distributed in a manner that at least 2/3 is still available

        // Find the most used avz
        int mostUsed = 0;
        int minUsed = Integer.MAX_VALUE;
        for (Map.Entry<AvailabilityZone, Integer> availabilityZoneRepartitionEntry : availabilityZoneRepartition.entrySet()) {
            if (availabilityZoneRepartitionEntry.getValue() > mostUsed) {
                mostUsed = availabilityZoneRepartitionEntry.getValue();
            }
            if (availabilityZoneRepartitionEntry.getValue() < minUsed) {
                minUsed = availabilityZoneRepartitionEntry.getValue();
            }
        }
        Assert.assertTrue(mostUsed - minUsed <= 1);
    }
}
