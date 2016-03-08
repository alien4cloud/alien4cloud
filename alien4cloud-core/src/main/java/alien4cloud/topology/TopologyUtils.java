package alien4cloud.topology;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.ScalingPolicy;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.tosca.normative.NormativeComputeConstants;

import com.google.common.collect.Maps;

public class TopologyUtils {

    private TopologyUtils() {
    }

    /**
     * Extract all interfaces that have given operations, and filter out all operations that are not in the includedOperations
     * 
     * @param interfaces interfaces to filter
     * @param includedOperations operations that will be included in the result
     * @return filter interfaces
     */
    public static Map<String, Interface> filterInterfaces(Map<String, Interface> interfaces, Set<String> includedOperations) {
        Map<String, Interface> result = Maps.newHashMap();
        for (Map.Entry<String, Interface> interfaceEntry : interfaces.entrySet()) {
            Map<String, Operation> operations = Maps.newHashMap();
            for (Map.Entry<String, Operation> operationEntry : interfaceEntry.getValue().getOperations().entrySet()) {
                if (includedOperations.contains(operationEntry.getKey())) {
                    operations.put(operationEntry.getKey(), operationEntry.getValue());
                }
            }
            if (!operations.isEmpty()) {
                Interface inter = new Interface();
                inter.setDescription(interfaceEntry.getValue().getDescription());
                inter.setOperations(operations);
                result.put(interfaceEntry.getKey(), inter);
            }
        }
        return result;
    }

    /**
     * Extract interfaces that have implemented operations only.
     *
     * @param allInterfaces all interfaces
     * @return interfaces that have implemented operations
     */
    public static Map<String, Interface> filterAbstractInterfaces(Map<String, Interface> allInterfaces) {
        Map<String, Interface> interfaces = Maps.newHashMap();
        for (Map.Entry<String, Interface> interfaceEntry : allInterfaces.entrySet()) {
            Map<String, Operation> operations = Maps.newHashMap();
            for (Map.Entry<String, Operation> operationEntry : interfaceEntry.getValue().getOperations().entrySet()) {
                if (operationEntry.getValue().getImplementationArtifact() == null) {
                    // Don't consider operation which do not have any implementation artifact
                    continue;
                }
                operations.put(operationEntry.getKey(), operationEntry.getValue());
            }
            if (!operations.isEmpty()) {
                // At least one operation fulfill the criteria
                Interface inter = new Interface();
                inter.setDescription(interfaceEntry.getValue().getDescription());
                inter.setOperations(operations);
                interfaces.put(interfaceEntry.getKey(), inter);
            }
        }
        return interfaces;
    }

    public static void setNullScalingPolicy(NodeTemplate nodeTemplate, IndexedNodeType resourceType) {
        // FIXME Workaround to remove default scalable properties from compute
        if (ToscaUtils.isFromType(NormativeComputeConstants.COMPUTE_TYPE, resourceType)) {
            if (nodeTemplate.getCapabilities() != null) {
                Capability scalableCapability = nodeTemplate.getCapabilities().get(NormativeComputeConstants.SCALABLE);
                if (scalableCapability != null && scalableCapability.getProperties() != null) {
                    scalableCapability.getProperties().put(NormativeComputeConstants.SCALABLE_MIN_INSTANCES, null);
                    scalableCapability.getProperties().put(NormativeComputeConstants.SCALABLE_MAX_INSTANCES, null);
                    scalableCapability.getProperties().put(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, null);
                }
            }
        }
    }

    public static ScalingPolicy getScalingPolicy(Capability capability) {
        int initialInstances = getScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, capability);
        int minInstances = getScalingProperty(NormativeComputeConstants.SCALABLE_MIN_INSTANCES, capability);
        int maxInstances = getScalingProperty(NormativeComputeConstants.SCALABLE_MAX_INSTANCES, capability);
        return new ScalingPolicy(minInstances, maxInstances, initialInstances);
    }

    public static int getScalingProperty(String propertyName, Capability capability) {
        if (MapUtils.isEmpty(capability.getProperties())) {
            throw new NotFoundException("The capability scalable has no defined properties, verify your tosca-normative-type archive");
        }

        if (!capability.getProperties().containsKey(propertyName)) {
            throw new NotFoundException(propertyName + " property is not found in the the capability");
        }

        // default value is 1
        int propertyValue = 1;

        String rawPropertyValue = FunctionEvaluator.getScalarValue(capability.getProperties().get(propertyName));
        if (StringUtils.isNotBlank(rawPropertyValue)) {
            propertyValue = Integer.parseInt(rawPropertyValue);
        }
        return propertyValue;
    }

    public static void setScalingProperty(String propertyName, int propertyValue, Capability capability) {
        if (MapUtils.isEmpty(capability.getProperties())) {
            throw new NotFoundException("The capability scalable has no defined properties, verify your tosca-normative-type archive");
        }
        capability.getProperties().put(propertyName, new ScalarPropertyValue(String.valueOf(propertyValue)));
    }

    public static Capability getCapability(Topology topology, String nodeTemplateId, String capabilityName, boolean throwNotFoundException) {
        return getCapability(topology.getNodeTemplates(), nodeTemplateId, capabilityName, throwNotFoundException);
    }

    public static Capability getCapability(Map<String, NodeTemplate> nodes, String nodeTemplateId, String capabilityName, boolean throwNotFoundException) {
        NodeTemplate node = nodes.get(nodeTemplateId);
        if (node == null) {
            if (throwNotFoundException) {
                throw new NotFoundException("Node " + nodeTemplateId + " is not found in the topology");
            } else {
                return null;
            }
        }
        Map<String, Capability> capabilities = node.getCapabilities();
        if (MapUtils.isEmpty(capabilities)) {
            if (throwNotFoundException) {
                throw new NotFoundException("Node " + nodeTemplateId + " does not have any capability");
            } else {
                return null;
            }
        }
        Capability capability = node.getCapabilities().get(capabilityName);
        if (capability == null) {
            if (throwNotFoundException) {
                throw new NotFoundException("Node " + nodeTemplateId + " does not have the capability scalable");
            } else {
                return null;
            }
        }
        return capability;
    }

    public static Capability getScalableCapability(Topology topology, String nodeTemplateId, boolean throwNotFoundException) {
        return getCapability(topology, nodeTemplateId, NormativeComputeConstants.SCALABLE, throwNotFoundException);
    }

    public static Capability getScalableCapability(Map<String, NodeTemplate> nodes, String nodeTemplateId, boolean throwNotFoundException) {
        return getCapability(nodes, nodeTemplateId, NormativeComputeConstants.SCALABLE, throwNotFoundException);
    }

    public static int getAvailableGroupIndex(Topology topology) {
        if (topology == null || topology.getGroups() == null) {
            return 0;
        }
        Collection<NodeGroup> nodeGroups = topology.getGroups().values();
        LinkedHashSet<Integer> indexSet = new LinkedHashSet<>(nodeGroups.size());
        for (int i = 0; i < nodeGroups.size(); i++) {
            indexSet.add(i);
        }
        for (NodeGroup nodeGroup : nodeGroups) {
            indexSet.remove(nodeGroup.getIndex());
        }
        if (indexSet.isEmpty()) {
            return nodeGroups.size();
        }
        return indexSet.iterator().next();
    }
}
