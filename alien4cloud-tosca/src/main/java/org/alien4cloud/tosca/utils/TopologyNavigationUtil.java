package org.alien4cloud.tosca.utils;

import static alien4cloud.utils.AlienUtils.safe;
import static org.alien4cloud.tosca.utils.ToscaTypeUtils.isOfType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.*;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Sets;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.PropertyUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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

    public static int getDefaultInstanceCount(Topology topology, NodeTemplate template, int multiplicator) {
        Capability scalableCapability = TopologyUtils.getScalableCapability(topology, template.getName(), false);
        int defaultInstanceCount = 1;
        if (scalableCapability != null) {
            ScalingPolicy scalingPolicy = TopologyUtils.getScalingPolicy(scalableCapability);
            if (!ScalingPolicy.NOT_SCALABLE_POLICY.equals(scalingPolicy)) {
                defaultInstanceCount = scalingPolicy.getInitialInstances();
            }
        }
        // now look for the host
        NodeTemplate host = getImmediateHostTemplate(topology, template);
        if (host != null) {
            return getDefaultInstanceCount(topology, host, multiplicator * defaultInstanceCount);
        } else {
            return multiplicator * defaultInstanceCount;
        }
    }

    public static NodeTemplate getImmediateHostTemplate(Topology topology, NodeTemplate template, IToscaTypeFinder toscaTypeFinder) {
        RelationshipTemplate host = getRelationshipFromType(template, NormativeRelationshipConstants.HOSTED_ON,
                id -> toscaTypeFinder.findElement(RelationshipType.class, id));
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
        return getRelationshipFromType(template, type, id -> ToscaContext.getOrFail(RelationshipType.class, id));
    }

    public static RelationshipTemplate getRelationshipFromType(NodeTemplate template, String type, IRelationshipTypeFinder toscaTypeFinder) {
        for (RelationshipTemplate relationshipTemplate : safe(template.getRelationships()).values()) {
            RelationshipType relationshipType = toscaTypeFinder.findElement(relationshipTemplate.getType());
            if (relationshipType != null && ToscaTypeUtils.isOfType(relationshipType, type)) {
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
                if (isOfType(relationshipType, NormativeRelationshipConstants.HOSTED_ON)) {
                    NodeTemplate hostNode = topology.getNodeTemplates().get(relationshipTemplate.getTarget());
                    NodeType hostNodeType = ToscaContext.get(NodeType.class, hostNode.getType());
                    if (isOfType(hostNodeType, hostType)) {
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
     *
     * For performance consideration, we don't factorise with {@link this#getNodesOfType(Topology, String, boolean)}.
     */
    public static Set<NodeTemplate> getNodesOfType(Topology topology, String type, boolean manageInheritance, boolean includeAbstractNodes) {
        Set<NodeTemplate> result = Sets.newHashSet();
        for (NodeTemplate nodeTemplate : safe(topology.getNodeTemplates()).values()) {
            NodeTemplate candidate = null;
            NodeType nodeType = ToscaContext.get(NodeType.class, nodeTemplate.getType());
            if (nodeTemplate.getType().equals(type)) {
                candidate = nodeTemplate;
            } else if (manageInheritance) {
                if (nodeType.getDerivedFrom().contains(type)) {
                    candidate = nodeTemplate;
                }
            }
            if (candidate != null) {
                if (includeAbstractNodes || !nodeType.isAbstract()) {
                    result.add(candidate);
                }
            }
        }
        return result;
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
     * @param topology
     * @param type
     * @param manageInheritance true if you also want to consider type hierarchy (ie. include that inherit the given type).
     * @return a set of nodes that are of the given type (or inherit the given type if <code>manageInheritance</code> is true).
     */
    public static Set<PolicyTemplate> getPoliciesOfType(Topology topology, String type, boolean manageInheritance) {
        Set<PolicyTemplate> result = Sets.newHashSet();
        for (PolicyTemplate policyTemplate : safe(topology.getPolicies()).values()) {
            if (policyTemplate.getType().equals(type)) {
                result.add(policyTemplate);
            } else if (manageInheritance) {
                PolicyType policyType = ToscaContext.get(PolicyType.class, policyTemplate.getType());
                if (policyType.getDerivedFrom().contains(type)) {
                    result.add(policyTemplate);
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
                .filter(policyTemplate -> policyTemplate.getTargets() != null && policyTemplate.getTargets().contains(nodeTemplate.getName()))
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
        for (RelationshipTemplate relationshipTemplate : getTargetRelationships(nodeTemplate, requirementName)) {
            result.add(topology.getNodeTemplates().get(relationshipTemplate.getTarget()));
        }
        return result;
    }

    /**
     * Returns all the relationships wired since this this node template's requirement.
     */
    public static Set<RelationshipTemplate> getTargetRelationships(NodeTemplate nodeTemplate, String requirementName) {
        Set<RelationshipTemplate> result = Sets.newHashSet();
        for (RelationshipTemplate relationshipTemplate : safe(nodeTemplate.getRelationships()).values()) {
            if (relationshipTemplate.getRequirementName().equals(requirementName)) {
                result.add(relationshipTemplate);
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
                if (relationshipTemplate.getTargetedCapabilityName().equals(capabilityName)
                        && relationshipTemplate.getTarget().equals(nodeTemplate.getName())) {
                    result.add(node);
                }
            }
        }
        return result;
    }

    /**
     * Returns all the nodes that target this node template with a relationship of the given type.
     */
    public static Set<NodeTemplate> getSourceNodesByRelationshipType(Topology topology, NodeTemplate nodeTemplate, String relationshipTypeName) {
        Set<NodeTemplate> result = Sets.newHashSet();
        for (NodeTemplate node : topology.getNodeTemplates().values()) {
            for (RelationshipTemplate relationshipTemplate : safe(node.getRelationships()).values()) {

                if (relationshipTemplate.getTarget().equals(nodeTemplate.getName())) {
                    RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, relationshipTemplate.getType());
                    if (ToscaTypeUtils.isOfType(relationshipType, relationshipTypeName)) {
                        result.add(node);
                    }
                }
            }
        }
        return result;
    }

    public static boolean hasRelationship(NodeTemplate sourceNode, String targetNodeName, String requirementName, String capabilityName) {
        for (RelationshipTemplate relationshipTemplate : safe(sourceNode.getRelationships()).values()) {
            if (relationshipTemplate.getTarget().equals(targetNodeName) && relationshipTemplate.getRequirementName().equals(requirementName)
                    && relationshipTemplate.getTargetedCapabilityName().equals(capabilityName)) {
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

    public static List<NodeTemplate> getHostedNodes(Topology topology, String nodeName) {
        return safe(topology.getNodeTemplates()).values().stream().filter(nodeTemplate -> safe(nodeTemplate.getRelationships()).values().stream()
                .anyMatch(relTemp -> relTemp.getTarget().equals(nodeName) && isHostedOnRelationship(relTemp.getType()))).collect(Collectors.toList());
    }

    private static boolean isHostedOnRelationship(String type) {
        RelationshipType relationshipType = ToscaContext.getOrFail(RelationshipType.class, type);
        return ToscaTypeUtils.isOfType(relationshipType, NormativeRelationshipConstants.HOSTED_ON);
    }

}
