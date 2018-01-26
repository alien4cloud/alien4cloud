package org.alien4cloud.tosca.editor.processors.nodetemplate;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.operations.nodetemplate.DuplicateNodeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;
import org.alien4cloud.tosca.utils.TopologyUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.paas.wf.TopologyContext;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.CloneUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Duplicate node template processor. <br>
 * If the node is a host, then copy along with it all hosted nodes.<br>
 * Discard any relationship that targets a node out of the duplicated hosted hierarchy.
 */
@Slf4j
@Component
public class DuplicateNodeProcessor implements IEditorOperationProcessor<DuplicateNodeOperation> {
    @Inject
    private TopologyService topologyService;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    public void process(Csar csar, Topology topology, DuplicateNodeOperation operation) {
        Map<String, NodeTemplate> nodeTemplates = TopologyUtils.getNodeTemplates(topology);
        // Retrieve existing node template
        NodeTemplate nodeTemplateToDuplicate = TopologyUtils.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);

        // map that will contains a mapping of the duplicated node and their new names
        Map<String, String> duplicatedNodesNameMappings = Maps.newHashMap();

        // first duplicate the node templates
        duplicateNodeTemplate(nodeTemplateToDuplicate, duplicatedNodesNameMappings, nodeTemplates, topology, csar);

        // then clean the relationships, discarding all that targets a node not in hostedNodes
        processRelationships(duplicatedNodesNameMappings, nodeTemplates, topology, csar);
    }

    /**
     * Process relationships of the duplicated nodes: Copy what we want to keep and discard the others
     *
     * @param duplicatedNodes Map of nodeToDuplicateName--> duplicatedNodeName
     * @param nodeTemplates
     * @param topology
     */
    private void processRelationships(Map<String, String> duplicatedNodes, Map<String, NodeTemplate> nodeTemplates, Topology topology, Csar csar) {
        TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology, csar);
        duplicatedNodes.values().forEach(nodeName -> copyAndCleanRelationships(nodeName, duplicatedNodes, nodeTemplates, topologyContext));
    }

    private void duplicateNodeTemplate(NodeTemplate nodeTemplateToDuplicate, Map<String, String> duplicatedNodesNameMappings,
            Map<String, NodeTemplate> nodeTemplates, Topology topology, Csar csar) {
        // Build the new one
        NodeTemplate newNodeTemplate = CloneUtil.clone(nodeTemplateToDuplicate);
        newNodeTemplate.setName(copyName(nodeTemplateToDuplicate.getName(), nodeTemplates.keySet()));

        // load type
        NodeType type = ToscaContext.getOrFail(NodeType.class, nodeTemplateToDuplicate.getType());
        topologyService.loadType(topology, type);

        log.debug("Duplicating node template [ {} ] into [ {} ] on the topology [ {} ] .", nodeTemplateToDuplicate.getName(), newNodeTemplate.getName(),
                topology.getId());
        // Put the new one in the topology
        nodeTemplates.put(newNodeTemplate.getName(), newNodeTemplate);

        // register the name mapping for further use
        duplicatedNodesNameMappings.put(nodeTemplateToDuplicate.getName(), newNodeTemplate.getName());

        // copy outputs
        copyOutputs(topology, nodeTemplateToDuplicate.getName(), newNodeTemplate.getName());

        TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology, csar);

        // add the new node to the workflow
        workflowBuilderService.addNode(topologyContext, newNodeTemplate.getName());

        // copy hosted nodes
        safe(TopologyNavigationUtil.getHostedNodes(topology, nodeTemplateToDuplicate.getName()))
                .forEach(nodeTemplate -> duplicateNodeTemplate(nodeTemplate, duplicatedNodesNameMappings, nodeTemplates, topology, csar));

    }

    /**
     * Discard all relationship targeting an "external" node. External here in terms of the hostedOn hierarchy
     * Copy the valid ones
     * 
     * @param nodeName
     * @param validTargets A map of oldNodeName -> duplicatedNodeName, we should keep relationships targeting one of these nodes.
     */
    private void copyAndCleanRelationships(String nodeName, Map<String, String> validTargets, Map<String, NodeTemplate> nodeTemplates,
            TopologyContext topologyContext) {

        NodeTemplate nodeTemplate = nodeTemplates.get(nodeName);
        if (MapUtils.isNotEmpty(nodeTemplate.getRelationships())) {
            Map<String, RelationshipTemplate> relationships = nodeTemplate.getRelationships();
            Set<String> keys = Sets.newHashSet(relationships.keySet());
            for (String key : keys) {
                RelationshipTemplate rel = relationships.remove(key);
                // check if the target is from the valids one
                // If so, then rename it, its target and keep it
                if (validTargets.containsKey(rel.getTarget())) {
                    rel.setName(copyName(rel.getName(), relationships.keySet()));
                    rel.setTarget(validTargets.get(rel.getTarget()));
                    relationships.put(rel.getName(), rel);
                    workflowBuilderService.addRelationship(topologyContext, nodeName, rel.getName());
                }
            }
            if (relationships.isEmpty()) {
                nodeTemplate.setRelationships(null);
            }
        }
    }

    /**
     * Copy outputs of a node template in a topology
     */
    private void copyOutputs(Topology topology, String toCopy, String copyName) {
        // Output properties
        copyValue(topology.getOutputProperties(), toCopy, copyName);

        // output capabilities properties
        copyCapabilitiesOutputProperties(topology.getOutputCapabilityProperties(), toCopy, copyName);

        // output attributes
        copyValue(topology.getOutputAttributes(), toCopy, copyName);
    }

    private <V> void copyValue(Map<String, V> map, String keyName, String copyKeyName) {
        if (MapUtils.isEmpty(map)) {
            return;
        }

        if (map.containsKey(keyName)) {
            V value = map.get(keyName);
            map.put(copyKeyName, value != null ? CloneUtil.clone(value) : null);
        }
    }

    /**
     *
     * Hack to clone values of outputCapabilityProperties, as when using the clone method, it deserialize Set into Array
     * 
     * @param map
     * @param keyName
     * @param copyKeyName
     */

    private void copyCapabilitiesOutputProperties(Map<String, Map<String, Set<String>>> map, String keyName, String copyKeyName) {
        if (MapUtils.isEmpty(map)) {
            return;
        }
        if (map.containsKey(keyName)) {
            Map<String, Set<String>> value = map.get(keyName);
            if (value != null) {
                Map<String, Set<String>> newValue = Maps.newHashMap();
                for (Map.Entry<String, Set<String>> entry : value.entrySet()) {
                    newValue.put(entry.getKey(), Sets.newHashSet(entry.getValue()));
                }
                map.put(copyKeyName, newValue);
            } else {
                map.put(copyKeyName, null);
            }
        }
    }

    private String copyName(String name, Collection<String> existingSet) {
        return TopologyUtils.getNexAvailableName(name + "_" + "copy", "", safe(existingSet));
    }

}
