package org.alien4cloud.tosca.utils;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.ScalingPolicy;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.RelationshipWorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.alien4cloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.Node;

import com.google.common.collect.Maps;

import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.NameValidationUtils;
import alien4cloud.utils.PropertyUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

        String rawPropertyValue = PropertyUtil.getScalarValue(capability.getProperties().get(propertyName));
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

    // private static Capability getCapability(Topology topology, String nodeTemplateId, String capabilityName, boolean throwNotFoundException) {
    // return getCapability(topology.getNodeTemplates(), nodeTemplateId, capabilityName, throwNotFoundException);
    // }
    //
    // private static Capability getCapability(Map<String, NodeTemplate> nodes, String nodeTemplateId, String capabilityName, boolean throwNotFoundException) {
    // NodeTemplate node = nodes.get(nodeTemplateId);
    // if (node == null) {
    // if (throwNotFoundException) {
    // throw new NotFoundException("Node " + nodeTemplateId + " is not found in the topology");
    // } else {
    // return null;
    // }
    // }
    // Capability capability = safe(node.getCapabilities()).get(capabilityName);
    // if (capability == null && throwNotFoundException) {
    // throw new NotFoundException("Node " + nodeTemplateId + " does not have the capability scalable");
    // }
    // return capability;
    // }

    public static Capability getScalableCapability(Topology topology, String nodeTemplateId, boolean throwNotFoundException) {
        NodeTemplate nodeTemplate = throwNotFoundException ? getNodeTemplate(topology, nodeTemplateId) : safe(topology.getNodeTemplates()).get(nodeTemplateId);
        if (nodeTemplate == null) {
            return null;
        }
        return throwNotFoundException ? NodeTemplateUtils.getCapabilityByTypeOrFail(nodeTemplate, NormativeCapabilityTypes.SCALABLE)
                : NodeTemplateUtils.getCapabilityByType(nodeTemplate, NormativeCapabilityTypes.SCALABLE);
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

    private static String toLowerCase(String text) {
        return text.substring(0, 1).toLowerCase() + text.substring(1);
    }

    private static String toUpperCase(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /**
     * Construct a relationship name from target and relationship type.
     *
     * @param type type of the relationship
     * @param targetName name of the target
     * @return the default constructed name
     */
    private static String getRelationShipName(String type, String targetName) {
        String[] tokens = type.split("\\.");
        if (tokens.length > 1) {
            return toLowerCase(tokens[tokens.length - 1]) + toUpperCase(targetName);
        } else {
            return toLowerCase(type) + toUpperCase(targetName);
        }
    }

    /**
     * Update properties in a topology
     */
    private static void updateOnNodeTemplateNameChange(String oldNodeTemplateName, String newNodeTemplateName, Topology topology) {
        // Output properties
        updateKey(topology.getOutputProperties(), oldNodeTemplateName, newNodeTemplateName);

        // output capabilities properties
        updateKey(topology.getOutputCapabilityProperties(), oldNodeTemplateName, newNodeTemplateName);

        // output attributes
        updateKey(topology.getOutputAttributes(), oldNodeTemplateName, newNodeTemplateName);

        // substitution mapping
        if (topology.getSubstitutionMapping() != null) {
            if (topology.getSubstitutionMapping().getCapabilities() != null) {
                for (SubstitutionTarget st : topology.getSubstitutionMapping().getCapabilities().values()) {
                    if (st.getNodeTemplateName().equals(oldNodeTemplateName)) {
                        st.setNodeTemplateName(newNodeTemplateName);
                    }
                }
            }
            if (topology.getSubstitutionMapping().getRequirements() != null) {
                for (SubstitutionTarget st : topology.getSubstitutionMapping().getRequirements().values()) {
                    if (st.getNodeTemplateName().equals(oldNodeTemplateName)) {
                        st.setNodeTemplateName(newNodeTemplateName);
                    }
                }
            }
        }
    }

    private static <V> void updateKey(Map<String, V> map, String oldKey, String newKey) {
        if (MapUtils.isEmpty(map)) {
            return;
        }

        V value = map.remove(oldKey);
        if (value != null) {
            map.put(newKey, value);
        }
    }

    /**
     * <p>
     * Update the name of a node template in the relationships of a topology.
     * This requires two operations:
     * <ul>
     * <li>Rename the target node of a relationship</li>
     * <li>If a relationship has an auto-generated id, update it's id to take in account the new target name.</li>
     * </ul>
     * </p>
     *
     * @param oldNodeTemplateName Name of the node template that changes.
     * @param newNodeTemplateName New name for the node template.
     * @param nodeTemplates Map of all node templates in the topology.
     */
    public static void refreshNodeTempNameInRelationships(String oldNodeTemplateName, String newNodeTemplateName, Map<String, NodeTemplate> nodeTemplates) {
        // node templates copy
        for (NodeTemplate nodeTemplate : nodeTemplates.values()) {
            if (nodeTemplate.getRelationships() != null) {
                refreshNodeTemplateNameInRelationships(oldNodeTemplateName, newNodeTemplateName, nodeTemplate.getRelationships());
            }
        }
    }

    private static void refreshNodeTemplateNameInRelationships(String oldNodeTemplateName, String newNodeTemplateName,
            Map<String, RelationshipTemplate> relationshipTemplates) {
        Map<String, String> updatedKeys = Maps.newHashMap();
        for (Map.Entry<String, RelationshipTemplate> relationshipTemplateEntry : relationshipTemplates.entrySet()) {
            String relationshipTemplateId = relationshipTemplateEntry.getKey();
            RelationshipTemplate relationshipTemplate = relationshipTemplateEntry.getValue();

            if (relationshipTemplate.getTarget().equals(oldNodeTemplateName)) {
                relationshipTemplate.setTarget(newNodeTemplateName);
                String formatedOldNodeName = getRelationShipName(relationshipTemplate.getType(), oldNodeTemplateName);
                // if the id/name of the relationship is auto-generated we should update it also as auto-generation is <typeName+targetId>
                if (relationshipTemplateId.equals(formatedOldNodeName)) {
                    // check that the new name is not already used (so we won't override another relationship)...
                    String validNewRelationshipTemplateId = getNexAvailableName(getRelationShipName(relationshipTemplate.getType(), newNodeTemplateName), "",
                            relationshipTemplates.keySet());
                    updatedKeys.put(relationshipTemplateId, validNewRelationshipTemplateId);
                }
            }
        }

        // update the relationship keys if any has been impacted
        for (Map.Entry<String, String> updateKeyEntry : updatedKeys.entrySet()) {
            RelationshipTemplate relationshipTemplate = relationshipTemplates.remove(updateKeyEntry.getKey());
            relationshipTemplates.put(updateKeyEntry.getValue(), relationshipTemplate);
        }
    }

    /**
     * Manage node group members when a node name is removed or its name has changed.
     *
     * @param newName : the new name of the node or <code>null</code> if the node has been removed.
     */
    public static void updateGroupMembers(Topology topology, NodeTemplate template, String nodeName, String newName) {
        Map<String, NodeGroup> topologyGroups = topology.getGroups();
        if (template.getGroups() != null && !template.getGroups().isEmpty() && topologyGroups != null) {
            for (String groupId : template.getGroups()) {
                NodeGroup nodeGroup = topologyGroups.get(groupId);
                if (nodeGroup != null && nodeGroup.getMembers() != null) {
                    boolean removed = nodeGroup.getMembers().remove(nodeName);
                    if (removed && newName != null) {
                        nodeGroup.getMembers().add(newName);
                    }
                }
            }
        }
    }

    /**
     * Rename formattedOldNodeName node template of a topology.
     * 
     * @param topology
     * @param nodeTemplateName
     * @param newNodeTemplateName
     */
    public static void renameNodeTemplate(Topology topology, String nodeTemplateName, String newNodeTemplateName) {
        Map<String, NodeTemplate> nodeTemplates = getNodeTemplates(topology);
        NodeTemplate nodeTemplate = getNodeTemplate(topology.getId(), nodeTemplateName, nodeTemplates);

        nodeTemplate.setName(newNodeTemplateName);
        nodeTemplates.put(newNodeTemplateName, nodeTemplate);
        nodeTemplates.remove(nodeTemplateName);

        refreshNodeTempNameInRelationships(nodeTemplateName, newNodeTemplateName, nodeTemplates);
        updateOnNodeTemplateNameChange(nodeTemplateName, newNodeTemplateName, topology);
        updateGroupMembers(topology, nodeTemplate, nodeTemplateName, newNodeTemplateName);
        updatePolicyMembers(topology, nodeTemplateName, newNodeTemplateName);
    }

    private static void updatePolicyMembers(Topology topology, String nodeName, String newName) {
        // Update the policy members when a node template is renamed.
        for (PolicyTemplate policyTemplate : safe(topology.getPolicies()).values()) {
            boolean removed = policyTemplate.getTargets().remove(nodeName);
            if (removed && newName != null) {
                policyTemplate.getTargets().add(newName);
            }
        }
    }

    /**
     * In alien 4 Cloud we try
     * Rename the node template with an invalid name on the topology.
     * 
     * @param topology
     * @param parsingErrors
     * @param objectToNodeMap
     */
    public static void normalizeAllNodeTemplateName(Topology topology, List<ParsingError> parsingErrors, Map<Object, Node> objectToNodeMap) {
        if (topology.getNodeTemplates() != null && !topology.getNodeTemplates().isEmpty()) {
            Map<String, NodeTemplate> nodeTemplates = Maps.newHashMap(topology.getNodeTemplates());
            for (Map.Entry<String, NodeTemplate> nodeEntry : nodeTemplates.entrySet()) {
                String nodeName = nodeEntry.getKey();
                if (!NameValidationUtils.isValid(nodeName)) {
                    String newName = StringUtils.stripAccents(nodeName);
                    newName = NameValidationUtils.DEFAULT_NAME_REPLACE_PATTERN.matcher(newName).replaceAll("_");
                    if (topology.getNodeTemplates().containsKey(newName)) {
                        int i = 1;
                        while (topology.getNodeTemplates().containsKey(newName + i)) {
                            i++;
                        }
                        newName = newName + i;
                    }
                    renameNodeTemplate(topology, nodeName, newName);
                    if (parsingErrors != null) {
                        Node node = (Node) MapUtil.get(objectToNodeMap, nodeName);
                        Mark startMark = null;
                        Mark endMark = null;
                        if (node != null) {
                            startMark = node.getStartMark();
                            endMark = node.getEndMark();
                        }
                        parsingErrors.add(
                                new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.INVALID_NAME, "NodeTemplate", startMark, nodeName, endMark, newName));
                    }
                }
            }
        }
    }

    /**
     * Get the Map of {@link NodeTemplate} from a topology
     *
     * @param topology the topology
     * @return this topology's node templates
     */
    public static Map<String, NodeTemplate> getNodeTemplates(Topology topology) {
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        if (nodeTemplates == null) {
            throw new NotFoundException("Topology [" + topology.getId() + "] do not have any node template");
        }
        return nodeTemplates;
    }

    /**
     * Get a {@link NodeTemplate} given its name from a map
     *
     * @param topologyId the topology's id
     * @param nodeTemplateName the name of the node template
     * @param nodeTemplates the topology's node templates
     * @return the found node template, throws NotFoundException if not found
     */
    public static NodeTemplate getNodeTemplate(String topologyId, String nodeTemplateName, Map<String, NodeTemplate> nodeTemplates) {
        NodeTemplate nodeTemplate = nodeTemplates.get(nodeTemplateName);
        if (nodeTemplate == null) {
            throw new NotFoundException("Topology [" + topologyId + "] do not have node template with name [" + nodeTemplateName + "]");
        }
        return nodeTemplate;
    }

    /**
     * Get a {@link NodeTemplate} given its name from a topology
     *
     * @param topology the topology
     * @param nodeTemplateId the name of the node template
     * @return the found node template, throws NotFoundException if not found
     */
    public static NodeTemplate getNodeTemplate(Topology topology, String nodeTemplateId) {
        Map<String, NodeTemplate> nodeTemplates = getNodeTemplates(topology);
        return getNodeTemplate(topology.getId(), nodeTemplateId, nodeTemplates);
    }

    public static String getNexAvailableName(String baseName, String separator, Collection<String> existingNames) {
        String name = baseName;
        int i = 1;
        while (existingNames.contains(name)) {
            name = baseName + separator + i;
            i++;
        }
        return name;
    }

    /**
     * Get all relationships that target the given node template.
     *
     * @param targetNodeTemplate the name of the node template which is target for relationship
     * @param nodeTemplates all topology's node templates
     * @return all relationships which have targetNodeTemplate as target
     */
    public static List<RelationshipEntry> getTargetRelationships(String targetNodeTemplate, Map<String, NodeTemplate> nodeTemplates) {
        List<RelationshipEntry> toReturn = Lists.newArrayList();
        for (String key : nodeTemplates.keySet()) {
            NodeTemplate nodeTemp = nodeTemplates.get(key);
            if (nodeTemp.getRelationships() == null) {
                continue;
            }
            for (String key2 : nodeTemp.getRelationships().keySet()) {
                RelationshipTemplate relTemp = nodeTemp.getRelationships().get(key2);
                if (relTemp == null) {
                    continue;
                }
                if (relTemp.getTarget() != null && relTemp.getTarget().equals(targetNodeTemplate)) {
                    toReturn.add(new RelationshipEntry(nodeTemp, key2, relTemp));
                }
            }
        }

        return toReturn;
    }

    /**
     * For the given topology, <u>estimate</u> the number of workflow step instances regarding the scaling settings.
     * <p/>
     * This should only be used for monitoring considerations since it will return an exact count.
     *
     * @param topology
     * @return a map where key is the workflow name and the value the estimated count.
     */
    public static Map<String, Integer> estimateWorkflowStepInstanceCount(Topology topology) {
        Map<String, Integer> stepInstancesCountPerWorkflow = Maps.newHashMap();

        Map<String, Integer> nodeInstanceCount = Maps.newHashMap();

        // for each node, count the number of expected instances
        topology.getNodeTemplates().forEach((nodeName, nodeTemplate) -> {
            int instanceCount = TopologyNavigationUtil.getDefaultInstanceCount(topology, nodeTemplate, 1);
            nodeInstanceCount.put(nodeName, instanceCount);
        });


        topology.getWorkflows().forEach((workflowName, workflow) -> {
            // TODO: use scale information in order to manage scaled nodes
            final AtomicInteger stepInstanceCount = new AtomicInteger(0);
            workflow.getSteps().forEach((s, workflowStep) -> {
                // set state activity are not considered
                if (workflowStep.getActivity() instanceof SetStateWorkflowActivity) {
                    return;
                }
                if (workflowStep instanceof NodeWorkflowStep) {
                    NodeWorkflowStep nws = (NodeWorkflowStep)workflowStep;
                    Integer instanceCount = nodeInstanceCount.get(nws.getTarget());
                    if (instanceCount != null) {
                        stepInstanceCount.addAndGet(instanceCount);
                    } else {
                        stepInstanceCount.incrementAndGet();
                    }
                } else if (workflowStep instanceof RelationshipWorkflowStep) {
                    RelationshipWorkflowStep rws = (RelationshipWorkflowStep)workflowStep;
                    String sourceNodeId = rws.getTarget();
                    NodeTemplate sourceNodeTemplate = TopologyUtils.getNodeTemplate(topology, sourceNodeId);
                    String targetNodeId = sourceNodeTemplate.getRelationships().get(rws.getTargetRelationship()).getTarget();
                    Integer instanceCount = nodeInstanceCount.get((rws.getOperationHost().equals("SOURCE")) ? sourceNodeId : targetNodeId);
                    if (instanceCount != null) {
                        stepInstanceCount.addAndGet(instanceCount);
                    } else {
                        stepInstanceCount.incrementAndGet();
                    }
                } else {
                    stepInstanceCount.incrementAndGet();
                }
            });
            stepInstancesCountPerWorkflow.put(workflowName, stepInstanceCount.get());
        });
        return stepInstancesCountPerWorkflow;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class RelationshipEntry {
        private NodeTemplate source;
        private String relationshipId;
        private RelationshipTemplate relationship;
    }
}