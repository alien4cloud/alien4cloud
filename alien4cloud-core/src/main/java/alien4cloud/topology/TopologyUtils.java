package alien4cloud.topology;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.ScalingPolicy;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.tosca.normative.NormativeComputeConstants;

public class TopologyUtils {

    private TopologyUtils() {
    }

    public static ScalingPolicy getScalingPolicy(Capability capability) {
        int initialInstances = getScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, capability);
        int minInstances = getScalingProperty(NormativeComputeConstants.SCALABLE_MIN_INSTANCES, capability);
        int maxInstances = getScalingProperty(NormativeComputeConstants.SCALABLE_MAX_INSTANCES, capability);
        ScalingPolicy scalingPolicy = new ScalingPolicy(minInstances, maxInstances, initialInstances);
        return scalingPolicy;
    }

    public static int getScalingProperty(String propertyName, Capability capability) {
        if (MapUtils.isEmpty(capability.getProperties())) {
            throw new NotFoundException("The capability scalable has no defined properties, verify your tosca-normative-type archive");
        }
        String rawPropertyValue = FunctionEvaluator.getScalarValue(capability.getProperties().get(propertyName));
        if (StringUtils.isEmpty(rawPropertyValue)) {
            throw new NotFoundException(propertyName + " property is not found in the the capability");
        }
        int propertyValue = Integer.parseInt(rawPropertyValue);
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
}
