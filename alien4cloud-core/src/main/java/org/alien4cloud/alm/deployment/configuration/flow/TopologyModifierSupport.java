package org.alien4cloud.alm.deployment.configuration.flow;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.operations.nodetemplate.*;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.DeleteRelationshipOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.*;
import org.alien4cloud.tosca.editor.processors.relationshiptemplate.AddRelationshipProcessor;
import org.alien4cloud.tosca.editor.processors.relationshiptemplate.DeleteRelationshipProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.application.TopologyCompositionService;
import alien4cloud.model.common.Tag;
import alien4cloud.tosca.context.ToscaContext;

/**
 * Base class for topology modifiers that can helps adding nodes, setting properties, replacing nodes, adding relationships.
 */
public abstract class TopologyModifierSupport implements ITopologyModifier {

    /**
     * This tag name is used to alias attributes (duplicate attributes for exposed services, a way to manage exposed containers for example).
     */
    public static final String A4C_MODIFIER_TAG_EXPOSED_ATTRIBUTE_ALIAS= "a4c_modifier_exposed_attribute_alias";

    @Resource
    protected AddNodeProcessor addNodeProcessor;

    @Resource
    protected ReplaceNodeProcessor replaceNodeProcessor;

    @Resource
    protected AddRelationshipProcessor addRelationshipProcessor;

    @Resource
    protected DeleteRelationshipProcessor deleteRelationshipProcessor;

    @Resource
    protected UpdateNodePropertyValueProcessor updateNodePropertyValueProcessor;

    @Resource
    protected UpdateCapabilityPropertyValueProcessor updateCapabilityPropertyValueProcessor;

    @Resource
    protected DeleteNodeProcessor deleteNodeProcessor;

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
        addNodeOperation.setSkipAutoCompletion(true);
        addNodeProcessor.process(csar, topology, addNodeOperation);
        return topology.getNodeTemplates().get(nodeName);
    }

    protected RelationshipTemplate addRelationshipTemplate(Csar csar, Topology topology, NodeTemplate sourceNode, String targetNodeName,
            String relationshipTypeName, String requirementName, String capabilityName) {
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

    protected void removeRelationship(Csar csar, Topology topology, String sourceNodeName, String relationshipTemplateName) {
        DeleteRelationshipOperation deleteRelationshipOperation = new DeleteRelationshipOperation();
        deleteRelationshipOperation.setNodeName(sourceNodeName);
        deleteRelationshipOperation.setRelationshipName(relationshipTemplateName);
        deleteRelationshipProcessor.process(csar, topology, deleteRelationshipOperation);
    }

    protected NodeTemplate replaceNode(Csar csar, Topology topology, NodeTemplate node, String nodeType, String nodeVersion) {
        ReplaceNodeOperation replaceNodeOperation = new ReplaceNodeOperation();
        replaceNodeOperation.setNodeName(node.getName());
        replaceNodeOperation.setNewTypeId(nodeType + ":" + nodeVersion);
        replaceNodeOperation.setSkipAutoCompletion(true);
        replaceNodeProcessor.process(csar, topology, replaceNodeOperation);
        return topology.getNodeTemplates().get(node.getName());
    }

    /**
     * Add the propertyValue to the list at the given path (Only the last property of the path must be a list).
     */
    protected void appendNodePropertyPathValue(Csar csar, Topology topology, NodeTemplate nodeTemplate, String propertyPath,
            AbstractPropertyValue propertyValue) {
        setNodePropertyPathValue(csar, topology, nodeTemplate, propertyPath, propertyValue, true);
    }

    /**
     * Set the propertyValue at the given path (doesn't manage lists in the path).
     */
    protected void setNodePropertyPathValue(Csar csar, Topology topology, NodeTemplate nodeTemplate, String propertyPath, AbstractPropertyValue propertyValue) {
        setNodePropertyPathValue(csar, topology, nodeTemplate, propertyPath, propertyValue, false);
    }

    /**
     * Change policies that target the sourceTemplate and make them target the targetTemplate.
     *
     * TODO: move elsewhere ?
     */
    public static void changePolicyTarget(Topology topology, NodeTemplate sourceTemplate, NodeTemplate targetTemplate) {
        Set<PolicyTemplate> policies = TopologyNavigationUtil.getTargetedPolicies(topology, sourceTemplate);
        policies.forEach(policyTemplate -> {
            policyTemplate.getTargets().remove(sourceTemplate.getName());
            policyTemplate.getTargets().add(targetTemplate.getName());
        });
    }

    protected void setNodeCappabilityPropertyPathValue(Csar csar, Topology topology, NodeTemplate nodeTemplate, String capabilityName, String propertyPath, AbstractPropertyValue propertyValue,
                                                       boolean lastPropertyIsAList) {
        Map<String, AbstractPropertyValue> propertyValues = nodeTemplate.getCapabilities().get(capabilityName).getProperties();
        String nodePropertyName = feedPropertyValue(propertyValues, propertyPath, propertyValue, lastPropertyIsAList);
        Object nodePropertyValue = propertyValues.get(nodePropertyName);

        UpdateCapabilityPropertyValueOperation operation = new UpdateCapabilityPropertyValueOperation();
        operation.setCapabilityName(capabilityName);
        operation.setNodeName(nodeTemplate.getName());
        operation.setPropertyName(nodePropertyName);
        operation.setPropertyValue(propertyValue);
        // TODO: can be necessary to serialize value before setting it in case of different types
        operation.setPropertyValue(nodePropertyValue);
        updateCapabilityPropertyValueProcessor.process(csar, topology, operation);
    }


    private void setNodePropertyPathValue(Csar csar, Topology topology, NodeTemplate nodeTemplate, String propertyPath, AbstractPropertyValue propertyValue,
            boolean lastPropertyIsAList) {
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

    // TODO: move elsewhere ?
    public static String feedPropertyValue(Map propertyValues, String propertyPath, Object propertyValue, boolean lastPropertyIsAList) {
        String nodePropertyName = null;
        if (propertyPath.contains(".")) {
            String[] paths = propertyPath.split("\\.");
            nodePropertyName = paths[0];
            Map<String, Object> currentMap = null;
            for (int i = 0; i < paths.length; i++) {
                if (i == 0) {
                    Object currentPropertyValue = propertyValues.get(paths[i]);
                    if (currentPropertyValue != null && currentPropertyValue instanceof ComplexPropertyValue) {
                        currentMap = ((ComplexPropertyValue) currentPropertyValue).getValue();
                    } else {
                        // FIXME OVERRIDING PROP VALUE This overrides the nodePropertyName property value!!!. We should instead fail if currentPropertyValue not
                        // instanceof ComplexPropertyValue
                        // FIXME and do this only if currentPropertyValue is null
                        currentMap = Maps.newHashMap();
                        propertyValues.put(nodePropertyName, new ComplexPropertyValue(currentMap));
                    }
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
                    } else if (currentPropertyValueObj != null && currentPropertyValueObj instanceof ComplexPropertyValue) {
                        currentPropertyValue = ((ComplexPropertyValue)currentPropertyValueObj).getValue();
                    } else {
                        // FIXME Same as OVERRIDING PROP VALUE above
                        currentPropertyValue = Maps.newHashMap();
                        currentMap.put(paths[i], currentPropertyValue);
                    }
                    currentMap = currentPropertyValue;
                }
            }
        } else {
            nodePropertyName = propertyPath;
            propertyValues.put(nodePropertyName, propertyValue);
        }
        return nodePropertyName;
    }

    public static void feedMapOrComplexPropertyEntry(Object map, String name, Object value) {
        Map rootMap = null;
        if (map instanceof ComplexPropertyValue) {
            rootMap = ((ComplexPropertyValue)map).getValue();
        } else if (map instanceof Map) {
            rootMap = (Map)map;
        }
        rootMap.put(name, value);
    }

    public static void setNodeTagValue(AbstractTemplate template, String name, String value) {
        List<Tag> tags = template.getTags();
        if (tags == null) {
            tags = Lists.newArrayList();
            template.setTags(tags);
        }
        tags.add(new Tag(name, value));
    }

    public static  String getNodeTagValueOrNull(AbstractTemplate template, String name) {
        List<Tag> tags = template.getTags();
        if (tags != null) {
            Optional<Tag> first = tags.stream().filter(tag -> tag.getName().equals(name)).findFirst();
            if (first.isPresent()) {
                return first.get().getValue();
            }
        }
        return null;
    }

    // remove the node and all nested nodes (recursively)
    protected void removeNode(Topology topology, NodeTemplate nodeTemplate) {
        // keep track of the hosted nodes
        List<NodeTemplate> hostedNodes = TopologyNavigationUtil.getHostedNodes(topology, nodeTemplate.getName());
        Csar csar = new Csar(topology.getArchiveName(), topology.getArchiveVersion());
        DeleteNodeOperation deleteNodeOperation = new DeleteNodeOperation();
        deleteNodeOperation.setNodeName(nodeTemplate.getName());
        deleteNodeProcessor.process(csar, topology, deleteNodeOperation);
        // remove all hosted node also
        safe(hostedNodes).forEach(hostedNodeTemplate -> removeNode(topology, hostedNodeTemplate));
    }
}
