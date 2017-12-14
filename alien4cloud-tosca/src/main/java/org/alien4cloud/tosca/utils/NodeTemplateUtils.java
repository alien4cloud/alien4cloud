package org.alien4cloud.tosca.utils;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Map.Entry;

import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;

import com.google.common.collect.Maps;

import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;

/**
 * Utility class to work with a node template.
 */
public final class NodeTemplateUtils {
    private NodeTemplateUtils() {
    }

    /**
     * Get the first capability entry that match the given type.
     *
     * @param nodeTemplate The node template in witch to lookup for the capability.
     * @param capabilityTypeName The capability type to look for.
     * @return The capability matching the type or null if no capability of the requested type is defined.
     */
    public static Entry<String, Capability> getCapabilitEntryyByType(NodeTemplate nodeTemplate, String capabilityTypeName) {
        for (Entry<String, Capability> capabilityEntry : safe(nodeTemplate.getCapabilities()).entrySet()) {
            if (capabilityEntry.getValue().getType().equals(capabilityTypeName)) {
                return capabilityEntry;
            }
            // if the type does not strictly equals we should check the derived from element
            CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capabilityEntry.getValue().getType());
            if (ToscaTypeUtils.isOfType(capabilityType, capabilityTypeName)) {
                return capabilityEntry;
            }
        }
        return null;
    }

    /**
     * Get the first capability that match the given type.
     *
     * @param nodeTemplate The node template in witch to lookup for the capability.
     * @param capabilityTypeName The capability type to look for.
     * @return The capability matching the type or null if no capability of the requested type is defined.
     */
    public static Capability getCapabilityByType(NodeTemplate nodeTemplate, String capabilityTypeName) {
        Entry<String, Capability> capabilityEntry = getCapabilitEntryyByType(nodeTemplate, capabilityTypeName);
        return capabilityEntry == null ? null : capabilityEntry.getValue();
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

    /**
     * Set a capabiltiy on the node template (and initialize the capability maps if null).
     * 
     * @param nodeTemplate The node template in which to add the capability.
     * @param name The name of the capability to add.
     * @param capability The capability.
     */
    public static void setCapability(NodeTemplate nodeTemplate, String name, Capability capability) {
        if (nodeTemplate.getCapabilities() == null) {
            nodeTemplate.setCapabilities(Maps.newHashMap());
        }
        nodeTemplate.getCapabilities().put(name, capability);
    }

    /**
     * Get the number of relationships from a node template that are actually linked to the given requirement.
     *
     * @param nodeTemplate The node template for which to count relationships
     * @param requirementDefinition The requirement definition from the node template's type for which to count related relationships.
     * @return The number of relationships connected to the given requirement.
     */
    public static int countRelationshipsForRequirement(NodeTemplate nodeTemplate, RequirementDefinition requirementDefinition) {
        int count = 0;
        for (Map.Entry<String, RelationshipTemplate> relEntry : safe(nodeTemplate.getRelationships()).entrySet()) {
            if (relEntry.getValue().getRequirementName().equals(requirementDefinition.getId())
                    && relEntry.getValue().getRequirementType().equals(requirementDefinition.getType())) {
                count++;
            }
        }
        return count;
    }
}