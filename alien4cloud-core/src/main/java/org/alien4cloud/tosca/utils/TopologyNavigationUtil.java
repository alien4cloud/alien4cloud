package org.alien4cloud.tosca.utils;

import static alien4cloud.utils.AlienUtils.safe;

import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.utils.PropertyUtil;
import com.google.common.collect.Sets;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.*;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;

import alien4cloud.tosca.context.ToscaContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for navigation purpose operations on node templates.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TopologyNavigationUtil {

    /**
     * Utility method to find the immediate host template of the current given template.
     * 
     * @param template The template for wich to get the immediate host.
     * @return
     */
    public static NodeTemplate getImmediateHostTemplate(Topology topology, NodeTemplate template) {
        RelationshipTemplate host = getRelationshipFromType(template, NormativeRelationshipConstants.HOSTED_ON);
        if (host == null) {
            return null;
        }
        return topology.getNodeTemplates().get(host.getTarget());
    }

    /**
     * Get all incoming relationships of a node that match the requested type.
     * 
     * @param template The node template for which to get relationships.
     * @param type The expected type.
     * @return
     */
    public static RelationshipTemplate getRelationshipFromType(NodeTemplate template, String type) {
        for (RelationshipTemplate relationshipTemplate : safe(template.getRelationships()).values()) {
            RelationshipType relationshipType = ToscaContext.getOrFail(RelationshipType.class, relationshipTemplate.getType());
            if (relationshipType != null && (relationshipType.getElementId().equals(type) || relationshipType.getDerivedFrom().contains(type))) {
                return relationshipTemplate;
            }
        }
        return null;
    }

    /**
     * @return true if this node is hosted on another.
     */
    public static boolean isHosted(Topology topology, NodeTemplate nodeTemplate) {
        return getImmediateHostTemplate(topology, nodeTemplate) != null;
    }

    /**
     * Deeply explore the hosted_on hierarchy of the given node to find a node of the given type.
     */
    public static NodeTemplate getHostOfTypeInHostingHierarchy(Topology topology, NodeTemplate nodeTemplate, String hostType) {
        if (nodeTemplate.getRelationships() != null) {
            for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, relationshipTemplate.getType());
                if (WorkflowUtils.isOfType(relationshipType, NormativeRelationshipConstants.HOSTED_ON)) {
                    NodeTemplate hostNode = topology.getNodeTemplates().get(relationshipTemplate.getTarget());
                    NodeType hostNodeType = ToscaContext.get(NodeType.class, hostNode.getType());
                    if (WorkflowUtils.isOfType(hostNodeType, hostType)) {
                        return hostNode;
                    } else {
                        return getHostOfTypeInHostingHierarchy(topology, hostNode, hostType);
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param topology
     * @param type
     * @param manageInheritance true if you also want to consider type hierarchy (ie. include that inherit the given type).
     * @return a set of nodes that are of the given type (or inherit the given type if <code>manageInheritance</code> is true).
     */
    public static Set<NodeTemplate> getNodesOfType(Topology topology, String type, boolean manageInheritance) {
        Set<NodeTemplate> result = Sets.newHashSet();
        for (NodeTemplate nodeTemplate : safe(topology.getNodeTemplates()).values()) {
            if (nodeTemplate.getType().equals(type)) {
                result.add(nodeTemplate);
            } else if (manageInheritance) {
                NodeType nodeType = ToscaContext.get(NodeType.class, nodeTemplate.getType());
                if (nodeType.getDerivedFrom().contains(type)) {
                    result.add(nodeTemplate);
                }
            }
        }
        return result;
    }

    /**
     * Get the policies that target this node template.
     */
    public static Set<PolicyTemplate> getTargetedPolicies(Topology topology, NodeTemplate nodeTemplate) {
        return safe(topology.getPolicies()).values().stream()
                .filter(policyTemplate -> policyTemplate.getTargets() != null
                        && policyTemplate.getTargets().contains(nodeTemplate.getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Get the members targeted by this policy.
     */
    public static Set<NodeTemplate> getTargetedMembers(Topology topology, PolicyTemplate policyTemplate) {
        return CollectionUtils.isEmpty(policyTemplate.getTargets()) ? Sets.newHashSet()
                : safe(topology.getNodeTemplates()).values().stream().filter(nodeTemplate -> isPolicyTarget(nodeTemplate, policyTemplate))
                .collect(Collectors.toSet());
    }

    public static boolean isPolicyTarget(NodeTemplate nodeTemplate, PolicyTemplate policyTemplate) {
        return safe(policyTemplate.getTargets()).contains(nodeTemplate.getName());
    }

    /**
     * Returns all the nodes that this node template targets with the given requirement.
     */
    public static Set<NodeTemplate> getTargetNodes(Topology topology, NodeTemplate nodeTemplate, String requirementName) {
        Set<NodeTemplate> result = Sets.newHashSet();
        for (RelationshipTemplate relationshipTemplate : safe(nodeTemplate.getRelationships()).values()) {
            if (relationshipTemplate.getRequirementName().equals(requirementName)) {
                result.add(topology.getNodeTemplates().get(relationshipTemplate.getTarget()));
            }
        }
        return result;
    }

    /**
     * Returns all the nodes that target this node template with the given requirement.
     */
    public static Set<NodeTemplate> getSourceNodes(Topology topology, NodeTemplate nodeTemplate, String capabilityName) {
        Set<NodeTemplate> result = Sets.newHashSet();
        for (NodeTemplate node : topology.getNodeTemplates().values()) {
            for (RelationshipTemplate relationshipTemplate : safe(node.getRelationships()).values()) {
                if (relationshipTemplate.getTargetedCapabilityName().equals(capabilityName) && relationshipTemplate.getTarget().equals(nodeTemplate.getName())) {
                    result.add(node);
                }
            }
        }
        return result;
    }

    public static boolean hasRelationship(NodeTemplate sourceNode, String targetNodeName, String requirementName, String capabilityName) {
        for (RelationshipTemplate relationshipTemplate : safe(sourceNode.getRelationships()).values()) {
            if (relationshipTemplate.getTarget().equals(targetNodeName) && relationshipTemplate.getRequirementName().equals(requirementName) && relationshipTemplate.getTargetedCapabilityName().equals(capabilityName)) {
                return true;
            }
        }
        return false;
    }

    public static AbstractPropertyValue getNodeCapabilityPropertyValue(NodeTemplate nodeTemplate, String capabilityName, String propertyPath) {
        Capability capability = safe(nodeTemplate.getCapabilities()).get(capabilityName);
        if (capability != null) {
            return PropertyUtil.getPropertyValueFromPath(safe(capability.getProperties()), propertyPath);
        }
        return null;
    }

}
