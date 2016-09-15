//package alien4cloud.paas.ha;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import lombok.extern.slf4j.Slf4j;
//
//import org.junit.Assert;
//import org.junit.Test;
//
//import alien4cloud.model.deployment.DeploymentSetup;
//import alien4cloud.model.cloud.AvailabilityZone;
//import alien4cloud.model.cloud.CloudResourceMatcherConfig;
//import alien4cloud.model.components.AbstractPropertyValue;
//import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
//import org.alien4cloud.tosca.model.templates.NodeTemplate;
//import alien4cloud.paas.model.PaaSNodeTemplate;
//import alien4cloud.paas.model.PaaSTopology;
//import alien4cloud.tosca.normative.NormativeBlockStorageConstants;
//
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import com.google.common.collect.Sets;
//
//@Slf4j
//public class AvailabilityZoneAllocatorTest {
//
//    public static final String HA_GROUP = "HA_group";
//
//    private List<PaaSNodeTemplate> generateComputes(int number) {
//        List<PaaSNodeTemplate> nodes = Lists.newArrayList();
//        for (int i = 0; i < number; i++) {
//            PaaSNodeTemplate node = new PaaSNodeTemplate("Compute" + i, null);
//            nodes.add(node);
//        }
//        return nodes;
//    }
//
//    private Set<AvailabilityZone> generateAvailabilityZones(int number) {
//        Set<AvailabilityZone> zones = Sets.newHashSet();
//        for (int i = 0; i < number; i++) {
//            AvailabilityZone zone = new AvailabilityZone("zone" + i, "Zone " + i);
//            zones.add(zone);
//        }
//        return zones;
//    }
//
//    private CloudResourceMatcherConfig generateCloudResourceMatcherConfig(Set<AvailabilityZone> zones) {
//        Map<AvailabilityZone, String> mapping = Maps.newHashMap();
//        for (AvailabilityZone zone : zones) {
//            mapping.put(zone, zone.getId());
//        }
//        CloudResourceMatcherConfig config = new CloudResourceMatcherConfig();
//        config.setAvailabilityZoneMapping(mapping);
//        return config;
//    }
//
//    @Test
//    public void test() {
//        doTest(generateComputes(2), generateAvailabilityZones(2), null);
//        doTest(generateComputes(4), generateAvailabilityZones(2), null);
//        doTest(generateComputes(3), generateAvailabilityZones(4), null);
//        doTest(generateComputes(5), generateAvailabilityZones(3), null);
//        doTest(generateComputes(7), generateAvailabilityZones(2), null);
//        doTest(generateComputes(7), generateAvailabilityZones(3), null);
//    }
//
//    @Test
//    public void testAllocationWithVolumeConfigured() {
//        List<PaaSNodeTemplate> computes = generateComputes(5);
//        Set<AvailabilityZone> zones = generateAvailabilityZones(2);
//        Map<String, PaaSNodeTemplate> volumes = Maps.newHashMap();
//        for (PaaSNodeTemplate compute : computes) {
//            NodeTemplate wrappedVolume = new NodeTemplate(NormativeBlockStorageConstants.BLOCKSTORAGE_TYPE, Maps.<String, AbstractPropertyValue> newHashMap(),
//                    null, null, null, null, null);
//            wrappedVolume.getProperties().put(NormativeBlockStorageConstants.VOLUME_ID, new ScalarPropertyValue(zones.iterator().next().getId() + "/abcde"));
//            PaaSNodeTemplate volume = new PaaSNodeTemplate("volume_" + compute.getId(), wrappedVolume);
//            volume.setParent(compute);
//            compute.getStorageNodes().add(volume);
//            volumes.put(compute.getId(), volume);
//        }
//        try {
//            doTest(computes, zones, volumes);
//            Assert.fail("AVZ of volumes must prevail");
//        } catch (Exception e) {
//            log.info("Normal that it fails", e);
//            // Must have assertion failure
//        }
//        Map<AvailabilityZone, Integer> repartitionMap = Maps.newHashMap();
//        for (AvailabilityZone zone : zones) {
//            repartitionMap.put(zone, 0);
//        }
//        AvailabilityZoneAllocator allocator = new AvailabilityZoneAllocator();
//        for (PaaSNodeTemplate volume : volumes.values()) {
//            AvailabilityZone zone = allocator.getLeastUsedAvailabilityZone(repartitionMap);
//            volume.getNodeTemplate().getProperties().put(NormativeBlockStorageConstants.VOLUME_ID, new ScalarPropertyValue(zone.getId() + "/abcde"));
//        }
//        // Must not have assertion failure
//        doTest(computes, zones, volumes);
//    }
//
//    public void doTest(List<PaaSNodeTemplate> computes, Set<AvailabilityZone> availableZones, Map<String, PaaSNodeTemplate> volumes) {
//        Map<String, List<PaaSNodeTemplate>> groups = Maps.newHashMap();
//        groups.put(HA_GROUP, computes);
//        PaaSTopology topology = new PaaSTopology();
//        topology.setGroups(groups);
//        if (volumes != null) {
//            topology.setVolumes(Lists.newArrayList(volumes.values()));
//        }
//        DeploymentSetup deploymentSetup = new DeploymentSetup();
//        Map<String, Set<AvailabilityZone>> availabilityZoneMapping = Maps.newHashMap();
//        availabilityZoneMapping.put(HA_GROUP, availableZones);
//        deploymentSetup.setAvailabilityZoneMapping(availabilityZoneMapping);
//        AvailabilityZoneAllocator allocator = new AvailabilityZoneAllocator();
//        Map<String, AvailabilityZone> allocated = allocator.processAllocation(topology, deploymentSetup, generateCloudResourceMatcherConfig(availableZones));
//
//        Map<AvailabilityZone, Integer> availabilityZoneRepartition = Maps.newHashMap();
//        for (AvailabilityZone availabilityZone : availableZones) {
//            availabilityZoneRepartition.put(availabilityZone, 0);
//        }
//
//        for (Map.Entry<String, AvailabilityZone> allocatedEntry : allocated.entrySet()) {
//            int usedCount = availabilityZoneRepartition.get(allocatedEntry.getValue());
//            availabilityZoneRepartition.put(allocatedEntry.getValue(), usedCount + 1);
//        }
//
//        // The goal is to prove that with 3 availability zones, when 1 crash
//        // We'll have computes distributed in a manner that at least 2/3 is still available
//
//        // Find the most used avz
//        int mostUsed = 0;
//        int leastUsed = Integer.MAX_VALUE;
//        for (Map.Entry<AvailabilityZone, Integer> availabilityZoneRepartitionEntry : availabilityZoneRepartition.entrySet()) {
//            if (availabilityZoneRepartitionEntry.getValue() > mostUsed) {
//                mostUsed = availabilityZoneRepartitionEntry.getValue();
//            }
//            if (availabilityZoneRepartitionEntry.getValue() < leastUsed) {
//                leastUsed = availabilityZoneRepartitionEntry.getValue();
//            }
//        }
//        if (mostUsed - leastUsed > 1) {
//            throw new RuntimeException("Test failed as most used and min used difference is bigger than 1, zones are not distributed equally");
//        }
//    }
//}

