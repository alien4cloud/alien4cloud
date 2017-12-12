package org.alien4cloud.tosca.utils;

import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;

import alien4cloud.tosca.context.ToscaContext;

public final class NodeTypeUtils {
    private NodeTypeUtils() {
    }

    /**
     * Get the first capability that match the given type.
     *
     * @param nodeType The node type in witch to lookup for the capability.
     * @param capabilityTypeName The capability type to look for.
     * @return The capability definition matching the type or null if no capability of the requested type is defined.
     */
    public static CapabilityDefinition getCapabilityByType(NodeType nodeType, String capabilityTypeName) {
        for (CapabilityDefinition capability : safe(nodeType.getCapabilities())) {
            if (capability.getType().equals(capabilityTypeName)) {
                return capability;
            }
            // if the type does not strictly equals we should check the derived from element
            CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capability.getType());
            if (ToscaTypeUtils.isOfType(capabilityType, capabilityTypeName)) {
                return capability;
            }
        }
        return null;
    }
}