package org.alien4cloud.alm.deployment.configuration.flow;

import alien4cloud.application.TopologyCompositionService;
import alien4cloud.model.common.Tag;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.PropertyUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import org.alien4cloud.tosca.model.templates.*;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        for (NodeTemplate nodeTemplate : AlienUtils.safe(topology.getNodeTemplates()).values()) {
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
        for (RelationshipTemplate relationshipTemplate : AlienUtils.safe(nodeTemplate.getRelationships()).values()) {
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
            for (RelationshipTemplate relationshipTemplate : AlienUtils.safe(node.getRelationships()).values()) {
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
        String relationShipName = TopologyCompositionService.ensureNodeNameIsUnique(AlienUtils.safe(sourceNode.getRelationships()).keySet(), sourceNode.getName() + "_" + targetNodeName, 0);
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
        for (RelationshipTemplate relationshipTemplate : AlienUtils.safe(sourceNode.getRelationships()).values()) {
            if (relationshipTemplate.getTarget().equals(targetNodeName) && relationshipTemplate.getRequirementName().equals(requirementName) && relationshipTemplate.getTargetedCapabilityName().equals(capabilityName)) {
                return true;
            }
        }
        return false;
    }

    // TODO ALIEN-2589: move elsewhere
    @Deprecated
    public static AbstractPropertyValue getNodeCapabilityPropertyValue(NodeTemplate nodeTemplate, String capabilityName, String propertyPath) {
        Capability capability = AlienUtils.safe(nodeTemplate.getCapabilities()).get(capabilityName);
        if (capability != null) {
            return PropertyUtil.getPropertyValueFromPath(AlienUtils.safe(capability.getProperties()), propertyPath);
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

    private void setNodePropertyPathValue(Csar csar, Topology topology, NodeTemplate nodeTemplate, String propertyPath, AbstractPropertyValue propertyValue, boolean lastPropertyIsAList) {
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

    protected void setNodeTagValue(AbstractTemplate template, String name, String value) {
        List<Tag> tags = template.getTags();
        if (tags == null) {
            tags = Lists.newArrayList();
            template.setTags(tags);
        }
        tags.add(new Tag(name, value));
    }

}
