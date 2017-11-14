package org.alien4cloud.tosca.utils;

import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;

import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;

/**
 * Utility class to work with a node template.
 */
public final class NodeTemplateUtils {
    private NodeTemplateUtils() {
    }

    /**
     * Get the first capability that match the given type.
     * 
     * @param nodeTemplate The node template in witch to lookup for the capability.
     * @param capabilityTypeName The capability type to look for.
     * @return The capability matching the type or null if no capability of the requested type is defined.
     */
    public static Capability getCapabilityByType(NodeTemplate nodeTemplate, String capabilityTypeName) {
        for (Capability capability : safe(nodeTemplate.getCapabilities()).values()) {
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

    /**
     * Get the first capability that match the given type.
     *
     * @param nodeTemplate The node template in witch to lookup for the capability.
     * @param capabilityType The capability type to look for.
     * @return The capability matching the type.
     * @throws NotFoundException if the capability is not found.
     */
    public static Capability getCapabilityByTypeOrFail(NodeTemplate nodeTemplate, String capabilityType) {
        Capability capability = getCapabilityByType(nodeTemplate, capabilityType);
        if (capability == null) {
            throw new NotFoundException("");
        }
        return capability;
    }
}