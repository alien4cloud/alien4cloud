package alien4cloud.paas.ha;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import alien4cloud.common.AlienConstants;
import alien4cloud.model.deployment.DeploymentSetup;
import alien4cloud.model.cloud.AvailabilityZone;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.paas.exception.AvailabilityZoneConfigurationException;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.tosca.normative.NormativeBlockStorageConstants;
import alien4cloud.utils.MappingUtil;

import com.google.common.collect.Lists;
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
     * @param cloudResourceMatcherConfig the resource matching configuration for this cloud
     * @return A map of compute node id --> Alien Availability Zone
     */
    public Map<String, AvailabilityZone> processAllocation(PaaSTopology topology, DeploymentSetup deploymentSetup,
            CloudResourceMatcherConfig cloudResourceMatcherConfig) {
        Map<String, AvailabilityZone> haComputeMap = Maps.newHashMap();
        if (topology.getVolumes() != null && !topology.getVolumes().isEmpty()) {
            Map<String, AvailabilityZone> paaSResourceIdToAvz = MappingUtil.getReverseMapping(cloudResourceMatcherConfig.getAvailabilityZoneMapping());
            List<PaaSNodeTemplate> volumes = topology.getVolumes();
            for (PaaSNodeTemplate volume : volumes) {
                PaaSNodeTemplate compute = volume.getParent();
                String avzId = getAvailabilityZone(volume);
                if (StringUtils.isNotEmpty(avzId)) {
                    AvailabilityZone avz = paaSResourceIdToAvz.get(avzId);
                    AvailabilityZone existingAvz = haComputeMap.put(compute.getId(), avz);
                    if (avz != null && existingAvz != null && !existingAvz.equals(avz)) {
                        log.warn("Cannot manage this use case : [" + compute.getId()
                                + "] have multiple volumes on different zones, only one availability zone will be selected");
                    }
                }
            }
        }
        Map<String, List<PaaSNodeTemplate>> groups = topology.getGroups();
        if (groups != null) {
            for (Map.Entry<String, List<PaaSNodeTemplate>> groupEntry : groups.entrySet()) {
                if (deploymentSetup.getAvailabilityZoneMapping() == null) {
                    throw new AvailabilityZoneConfigurationException(groupEntry.getKey(), "Need at least 1 availability zone configured to process allocation");
                }
                Collection<AvailabilityZone> availabilityZones = deploymentSetup.getAvailabilityZoneMapping().get(groupEntry.getKey());
                if (availabilityZones == null || availabilityZones.isEmpty()) {
                    throw new AvailabilityZoneConfigurationException(groupEntry.getKey(), "Need at least 1 availability zone configured to process allocation");
                }
                Map<AvailabilityZone, Integer> availabilityZoneRepartition = Maps.newHashMap();
                for (AvailabilityZone availabilityZone : availabilityZones) {
                    availabilityZoneRepartition.put(availabilityZone, 0);
                }
                for (PaaSNodeTemplate compute : groupEntry.getValue()) {
                    AvailabilityZone existingAvz = haComputeMap.get(compute.getId());
                    if (existingAvz != null) {
                        Integer existingCount = availabilityZoneRepartition.get(existingAvz);
                        if (existingCount == null) {
                            log.warn(groupEntry.getKey(), "Attention AVZ mapping has been changed, the AVZ " + existingAvz.getId()
                                    + " injected by the existing volume is no longer valid for this compute " + compute.getId());
                        } else {
                            availabilityZoneRepartition.put(existingAvz, existingCount + 1);
                        }
                    }
                }
                for (PaaSNodeTemplate compute : groupEntry.getValue()) {
                    AvailabilityZone existingAvz = haComputeMap.get(compute.getId());
                    if (existingAvz == null) {
                        haComputeMap.put(compute.getId(), getLeastUsedAvailabilityZone(availabilityZoneRepartition));
                    }
                }
            }
        }
        return haComputeMap;
    }

    public List<AllocationError> validateAllocation(Map<String, AvailabilityZone> allocation, PaaSTopology topology, DeploymentSetup deploymentSetup,
            CloudResourceMatcherConfig cloudResourceMatcherConfig) {
        List<AllocationError> allocationErrors = Lists.newArrayList();
        Map<AvailabilityZone, String> avzToPaaSResourceId = cloudResourceMatcherConfig.getAvailabilityZoneMapping();
        for (PaaSNodeTemplate compute : topology.getComputes()) {
            if (compute.getGroups() != null && compute.getGroups().size() > 1) {
                for (String groupId : compute.getGroups()) {
                    allocationErrors.add(new AllocationError(AllocationErrorCode.NODE_BELONG_TO_MULTIPLE_HA_GROUPS, groupId, compute.getId()));
                }
            }
        }

        for (Map.Entry<String, List<PaaSNodeTemplate>> groupEntry : topology.getGroups().entrySet()) {
            String groupId = groupEntry.getKey();
            List<PaaSNodeTemplate> groupComputes = groupEntry.getValue();
            Map<AvailabilityZone, Integer> repartitionMap = Maps.newHashMap();
            if (deploymentSetup.getAvailabilityZoneMapping() == null || !deploymentSetup.getAvailabilityZoneMapping().containsKey(groupId)) {
                throw new AvailabilityZoneConfigurationException(groupId, "Ask to validate allocation on a topology with invalid deployment setup");
            }
            for (AvailabilityZone matchedZone : deploymentSetup.getAvailabilityZoneMapping().get(groupId)) {
                repartitionMap.put(matchedZone, 0);
            }
            for (PaaSNodeTemplate groupCompute : groupComputes) {
                AvailabilityZone allocatedZone = allocation.get(groupCompute.getId());
                if (allocatedZone == null) {
                    allocationErrors.add(new AllocationError(AllocationErrorCode.NODE_NOT_ALLOCATED, groupId, groupCompute.getId()));
                    break;
                } else if (!areStoragesInTheZone(groupCompute.getStorageNodes(), avzToPaaSResourceId.get(allocatedZone))) {
                    allocationErrors.add(new AllocationError(AllocationErrorCode.NODE_HAS_VOLUME_NOT_IN_THE_SAME_ZONE, groupId, groupCompute.getId()));
                    break;
                }
                Integer existingCount = repartitionMap.get(allocatedZone);
                if (existingCount == null) {
                    allocationErrors
                            .add(new AllocationError(AllocationErrorCode.NODE_HAS_ALLOCATED_ZONE_NOT_IN_DEPLOYMENT_SETUP, groupId, groupCompute.getId()));
                } else {
                    repartitionMap.put(allocatedZone, existingCount + 1);
                }
            }
            int mostUsed = 0;
            int leastUsed = Integer.MAX_VALUE;
            for (Map.Entry<AvailabilityZone, Integer> availabilityZoneRepartitionEntry : repartitionMap.entrySet()) {
                if (availabilityZoneRepartitionEntry.getValue() > mostUsed) {
                    mostUsed = availabilityZoneRepartitionEntry.getValue();
                }
                if (availabilityZoneRepartitionEntry.getValue() < leastUsed) {
                    leastUsed = availabilityZoneRepartitionEntry.getValue();
                }
            }
            if (mostUsed - leastUsed > 1) {
                allocationErrors.add(new AllocationError(AllocationErrorCode.ZONES_NOT_DISTRIBUTED_EQUALLY, groupId, null));
            }
        }
        return allocationErrors;
    }

    private boolean areStoragesInTheZone(List<PaaSNodeTemplate> storageNodes, String zoneToCheck) {
        for (PaaSNodeTemplate volume : storageNodes) {
            String volumeAVZ = getAvailabilityZone(volume);
            if (volumeAVZ != null && !volumeAVZ.equals(zoneToCheck)) {
                return false;
            }
        }
        return true;
    }

    AvailabilityZone getLeastUsedAvailabilityZone(Map<AvailabilityZone, Integer> availabilityZoneRepartition) {
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

    String getAvailabilityZone(PaaSNodeTemplate volume) {
        Map<String, AbstractPropertyValue> volumeProperties = volume.getNodeTemplate().getProperties();
        if (volumeProperties != null && volumeProperties.containsKey(NormativeBlockStorageConstants.VOLUME_ID)) {
            String allVolumeIds = FunctionEvaluator.getScalarValue(volumeProperties.get(NormativeBlockStorageConstants.VOLUME_ID));
            if (StringUtils.isNotEmpty(allVolumeIds)) {
                int indexOfAvzAndIdSeparator = allVolumeIds.split(",")[0].indexOf(AlienConstants.STORAGE_AZ_VOLUMEID_SEPARATOR);
                // TODO for the moment we suppose we do not manage HA for node with scaling policy
                if (indexOfAvzAndIdSeparator > 0) {
                    return allVolumeIds.substring(0, indexOfAvzAndIdSeparator);
                }
            }
        }
        return null;
    }
}
