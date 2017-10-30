package org.alien4cloud.alm.deployment.configuration.flow;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation;
import org.alien4cloud.tosca.editor.operations.nodetemplate.ReplaceNodeOperation;
import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AddNodeProcessor;
import org.alien4cloud.tosca.editor.processors.nodetemplate.ReplaceNodeProcessor;
import org.alien4cloud.tosca.editor.processors.nodetemplate.UpdateNodePropertyValueProcessor;
import org.alien4cloud.tosca.editor.processors.relationshiptemplate.AddRelationshipProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.application.TopologyCompositionService;
import alien4cloud.model.common.Tag;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.PropertyUtil;

/**
 * Base class for topology modifiers that can helps adding nodes, setting properties, replacing nodes, adding relationships.
 */
public abstract class TopologyModifierSupport implements ITopologyModifier {

    @Resource
    protected AddNodeProcessor addNodeProcessor;

    @Resource
    protected ReplaceNodeProcessor replaceNodeProcessor;

    @Resource
    protected AddRelationshipProcessor addRelationshipProcessor;

    @Resource
    protected UpdateNodePropertyValueProcessor updateNodePropertyValueProcessor;

    /**
     * @return true if this node is hosted on another.
     */
    // TODO ALIEN-2589: move elsewhere
    // TODO ALIEN-2589: unit test
    @Deprecated
    public static boolean isHosted(Topology topology, NodeTemplate nodeTemplate) {
        return getHostNode(topology, nodeTemplate) != null;
    }

    /**
     * Return the node that hosts the given node, or null if none.
     * TODO ALIEN-2589: move elsewhere
     */
    @Deprecated
    public static NodeTemplate getHostNode(Topology topology, NodeTemplate nodeTemplate) {
        if (nodeTemplate.getRelationships() != null) {
            for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, relationshipTemplate.getType());
                if (WorkflowUtils.isOfType(relationshipType, NormativeRelationshipConstants.HOSTED_ON)) {
                    return topology.getNodeTemplates().get(relationshipTemplate.getTarget());
                }
            }
        }
        return null;
    }

    /**
     * Deeply explore the hosted_on hierarchy of the given node to find a node of the given type.
     * TODO ALIEN-2589: move elsewhere
     */
    @Deprecated
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
    // TODO ALIEN-2589: move elsewhere
    // TODO ALIEN-2589: unit test
    @Deprecated
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

    private static boolean isPolicyTarget(NodeTemplate nodeTemplate, PolicyTemplate policyTemplate) {
        return safe(policyTemplate.getTargets()).contains(nodeTemplate.getName());
    }

    /**
     * Change policies that target the sourceTemplate and make them target the targetTemplate.
     */
    public static void changePolicyTarget(Topology topology, NodeTemplate sourceTemplate, NodeTemplate targetTemplate) {
        Set<PolicyTemplate> policies = getTargetedPolicies(topology, sourceTemplate);
        policies.forEach(policyTemplate -> {
            policyTemplate.getTargets().remove(sourceTemplate.getName());
            policyTemplate.getTargets().add(targetTemplate.getName());
        });
    }

    /**
     * Add a node template in the topology.
     *
     * @param csar
     * @param topology
     * @param desiredNodeName the name you would like for this node (but can be suffixed if this name is already used).
     * @param nodeType
     * @param nodeVersion
     * @return the created node.
     */
    // TODO ALIEN-2589: unit test
    protected NodeTemplate addNodeTemplate(Csar csar, Topology topology, String desiredNodeName, String nodeType, String nodeVersion) {
        AddNodeOperation addNodeOperation = new AddNodeOperation();
        String nodeName = TopologyCompositionService.ensureNodeNameIsUnique(topology.getNodeTemplates().keySet(), desiredNodeName, 0);
        addNodeOperation.setNodeName(nodeName);
        addNodeOperation.setIndexedNodeTypeId(nodeType + ":" + nodeVersion);
        addNodeProcessor.process(csar, topology, addNodeOperation);
        return topology.getNodeTemplates().get(nodeName);
    }

    /**
     * Returns all the nodes that this node template targets with the given requirement.
     */
    // TODO ALIEN-2589: move elsewhere
    @Deprecated
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
    // TODO ALIEN-2589: move elsewhere
    @Deprecated
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

    protected RelationshipTemplate addRelationshipTemplate(Csar csar, Topology topology, NodeTemplate sourceNode, String targetNodeName, String relationshipTypeName, String requirementName, String capabilityName) {
        AddRelationshipOperation addRelationshipOperation = new AddRelationshipOperation();
        addRelationshipOperation.setNodeName(sourceNode.getName());
        addRelationshipOperation.setTarget(targetNodeName);
        RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, relationshipTypeName);
        addRelationshipOperation.setRelationshipType(relationshipType.getElementId());
        addRelationshipOperation.setRelationshipVersion(relationshipType.getArchiveVersion());
        addRelationshipOperation.setRequirementName(requirementName);
        addRelationshipOperation.setTargetedCapabilityName(capabilityName);
        String relationShipName = TopologyCompositionService.ensureNodeNameIsUnique(safe(sourceNode.getRelationships()).keySet(),
                sourceNode.getName() + "_" + targetNodeName, 0);
        addRelationshipOperation.setRelationshipName(relationShipName);
        addRelationshipProcessor.process(csar, topology, addRelationshipOperation);
        return sourceNode.getRelationships().get(relationShipName);
    }

    protected NodeTemplate replaceNode(Csar csar, Topology topology, NodeTemplate node, String nodeType, String nodeVersion) {
        ReplaceNodeOperation replaceNodeOperation = new ReplaceNodeOperation();
        replaceNodeOperation.setNodeName(node.getName());
        replaceNodeOperation.setNewTypeId(nodeType + ":" + nodeVersion);
        replaceNodeProcessor.process(csar, topology, replaceNodeOperation);
        return topology.getNodeTemplates().get(node.getName());
    }

    // TODO ALIEN-2589: move elsewhere
    @Deprecated
    public static boolean hasRelationship(NodeTemplate sourceNode, String targetNodeName, String requirementName, String capabilityName) {
        for (RelationshipTemplate relationshipTemplate : safe(sourceNode.getRelationships()).values()) {
            if (relationshipTemplate.getTarget().equals(targetNodeName) && relationshipTemplate.getRequirementName().equals(requirementName) && relationshipTemplate.getTargetedCapabilityName().equals(capabilityName)) {
                return true;
            }
        }
        return false;
    }

    // TODO ALIEN-2589: move elsewhere
    @Deprecated
    public static AbstractPropertyValue getNodeCapabilityPropertyValue(NodeTemplate nodeTemplate, String capabilityName, String propertyPath) {
        Capability capability = safe(nodeTemplate.getCapabilities()).get(capabilityName);
        if (capability != null) {
            return PropertyUtil.getPropertyValueFromPath(safe(capability.getProperties()), propertyPath);
        }
        return null;
    }

    /**
     * Add the propertyValue to the list at the given path (Only the last property of the path must be a list).
     */
    protected void appendNodePropertyPathValue(Csar csar, Topology topology, NodeTemplate nodeTemplate, String propertyPath, AbstractPropertyValue propertyValue) {
        setNodePropertyPathValue(csar, topology, nodeTemplate, propertyPath, propertyValue, true);
    }

    /**
     * Set the propertyValue at the given path (doesn't manage lists in the path).
     */
    protected void setNodePropertyPathValue(Csar csar, Topology topology, NodeTemplate nodeTemplate, String propertyPath, AbstractPropertyValue propertyValue) {
        setNodePropertyPathValue(csar, topology, nodeTemplate, propertyPath, propertyValue, false);
    }

    @Deprecated
    private void _setNodePropertyPathValue(Csar csar, Topology topology, NodeTemplate nodeTemplate, String propertyPath, AbstractPropertyValue propertyValue, boolean lastPropertyIsAList) {
        Object nodePropertyValue = null;
        String nodePropertyName = null;
        if (propertyPath.contains(".")) {
            String[] paths = propertyPath.split("\\.");
            nodePropertyName = paths[0];
            Map<String, AbstractPropertyValue> propertyValues = nodeTemplate.getProperties();
            Map<String, Object> currentMap = null;
            for (int i = 0; i < paths.length; i++) {
                if (i == 0) {
                    AbstractPropertyValue currentPropertyValue = propertyValues.get(paths[i]);
                    if (currentPropertyValue != null && currentPropertyValue instanceof ComplexPropertyValue) {
                        currentMap = ((ComplexPropertyValue) currentPropertyValue).getValue();
                    } else {
                        currentMap = Maps.newHashMap();
                    }
                    nodePropertyValue = currentMap;
                } else if (i == paths.length - 1) {
                    // TODO: find a better way to manage this
                    if (lastPropertyIsAList) {
                        Object currentEntry = currentMap.get(paths[i]);
                        ListPropertyValue listPropertyValue = null;
                        if (currentEntry != null && currentEntry instanceof ListPropertyValue) {
                            listPropertyValue = (ListPropertyValue) currentEntry;
                        } else {
                            listPropertyValue = new ListPropertyValue(Lists.newArrayList());
                            currentMap.put(paths[i], listPropertyValue);
                        }
                        listPropertyValue.getValue().add(propertyValue);
                    } else {
                        currentMap.put(paths[i], propertyValue);
                    }
                } else {
                    Map<String, Object> currentPropertyValue = null;
                    Object currentPropertyValueObj = currentMap.get(paths[i]);
                    if (currentPropertyValueObj != null && currentPropertyValueObj instanceof Map<?, ?>) {
                        currentPropertyValue = (Map<String, Object>) currentPropertyValueObj;
                    } else {
                        currentPropertyValue = Maps.newHashMap();
                        currentMap.put(paths[i], currentPropertyValue);
                    }
                    currentMap = currentPropertyValue;
                }
            }
        } else {
            nodePropertyValue = propertyValue;
            nodePropertyName = propertyPath;
        }

        UpdateNodePropertyValueOperation updateNodePropertyValueOperation = new UpdateNodePropertyValueOperation();
        updateNodePropertyValueOperation.setNodeName(nodeTemplate.getName());
        updateNodePropertyValueOperation.setPropertyName(nodePropertyName);
        // TODO: can be necessary to serialize value before setting it in case of different types
        updateNodePropertyValueOperation.setPropertyValue(nodePropertyValue);
        updateNodePropertyValueProcessor.process(csar, topology, updateNodePropertyValueOperation);
    }

    private void setNodePropertyPathValue(Csar csar, Topology topology, NodeTemplate nodeTemplate, String propertyPath, AbstractPropertyValue propertyValue, boolean lastPropertyIsAList) {
        Map<String, AbstractPropertyValue> propertyValues = nodeTemplate.getProperties();
        String nodePropertyName = feedPropertyValue(propertyValues, propertyPath, propertyValue, lastPropertyIsAList);
        Object nodePropertyValue = propertyValues.get(nodePropertyName);

        UpdateNodePropertyValueOperation updateNodePropertyValueOperation = new UpdateNodePropertyValueOperation();
        updateNodePropertyValueOperation.setNodeName(nodeTemplate.getName());
        updateNodePropertyValueOperation.setPropertyName(nodePropertyName);
        // TODO: can be necessary to serialize value before setting it in case of different types
        updateNodePropertyValueOperation.setPropertyValue(nodePropertyValue);
        updateNodePropertyValueProcessor.process(csar, topology, updateNodePropertyValueOperation);
    }

    protected String feedPropertyValue(Map<String, AbstractPropertyValue> propertyValues, String propertyPath, AbstractPropertyValue propertyValue, boolean lastPropertyIsAList) {
//        Object nodePropertyValue = null;
        String nodePropertyName = null;
        if (propertyPath.contains(".")) {
            String[] paths = propertyPath.split("\\.");
            nodePropertyName = paths[0];
//            Map<String, AbstractPropertyValue> propertyValues = nodeTemplate.getProperties();
            Map<String, Object> currentMap = null;
            for (int i = 0; i < paths.length; i++) {
                if (i == 0) {
                    AbstractPropertyValue currentPropertyValue = propertyValues.get(paths[i]); // path[i] == nodePropertyName
                    if (currentPropertyValue != null && currentPropertyValue instanceof ComplexPropertyValue) {
                        currentMap = ((ComplexPropertyValue) currentPropertyValue).getValue();
                    } else {
                        // FIXME OVERRIDING PROP VALUE This overrides the nodePropertyName property value!!!. We should instead fail if currentPropertyValue not
                        // instanceof ComplexPropertyValue
                        // FIXME and do this only if currentPropertyValue is null
                        currentMap = Maps.newHashMap();
                        propertyValues.put(nodePropertyName, new ComplexPropertyValue(currentMap));
                    }
//                    nodePropertyValue = currentMap;
                } else if (i == paths.length - 1) {
                    // TODO: find a better way to manage this
                    if (lastPropertyIsAList) {
                        Object currentEntry = currentMap.get(paths[i]);
                        ListPropertyValue listPropertyValue = null;
                        if (currentEntry != null && currentEntry instanceof ListPropertyValue) {
                            listPropertyValue = (ListPropertyValue) currentEntry;
                        } else {
                            // FIXME Same as OVERRIDING PROP VALUE above
                            listPropertyValue = new ListPropertyValue(Lists.newArrayList());
                            currentMap.put(paths[i], listPropertyValue);
                        }
                        listPropertyValue.getValue().add(propertyValue);
                    } else {
                        currentMap.put(paths[i], propertyValue);
                    }
                } else {
                    Map<String, Object> currentPropertyValue = null;
                    Object currentPropertyValueObj = currentMap.get(paths[i]);
                    if (currentPropertyValueObj != null && currentPropertyValueObj instanceof Map<?, ?>) {
                        currentPropertyValue = (Map<String, Object>) currentPropertyValueObj;
                    } else {
                        // FIXME Same as OVERRIDING PROP VALUE above
                        currentPropertyValue = Maps.newHashMap();
                        currentMap.put(paths[i], currentPropertyValue);
                    }
                    currentMap = currentPropertyValue;
                }
            }
        } else {
//            nodePropertyValue = propertyValue;
            nodePropertyName = propertyPath;
            propertyValues.put(nodePropertyName, propertyValue);
        }
        return nodePropertyName;
    }

    protected void setNodeTagValue(AbstractTemplate template, String name, String value) {
        List<Tag> tags = template.getTags();
        if (tags == null) {
            tags = Lists.newArrayList();
            template.setTags(tags);
        }
        tags.add(new Tag(name, value));
    }

}
