package org.alien4cloud.tosca.editor.processors.nodetemplate;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.CopyNodeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.ToscaNormativeUtil;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.CloneUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Copy a node template processor. <br>
 * If the node is a host, then copy along with it all hosted nodes.<br>
 * Discard any relationship that targets a node out of the copied hosted hierarchy.
 */
@Slf4j
@Component
public class CopyNodeProcessor implements IEditorOperationProcessor<CopyNodeOperation> {
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject
    private TopologyService topologyService;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    public void process(CopyNodeOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        Map<String, NodeTemplate> nodeTemplates = TopologyUtils.getNodeTemplates(topology);
        // Retrieve existing node template
        NodeTemplate nodeTemplateToCopy = TopologyUtils.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);

        // map that will contains a mapping of the copied node and their new names
        Map<String, String> copiedNodesNameMappings = Maps.newHashMap();

        // first copy the node templates
        copyNodeTemplate(nodeTemplateToCopy, copiedNodesNameMappings, nodeTemplates, topology);

        // then clean the relationships, discarding all that targets a node not in hostedNodes
        processRelationships(copiedNodesNameMappings, nodeTemplates, topology);
    }

    /**
     * Process relationships of the copied nodes: Copy what we want to keep and discard the others
     *
     * @param copiedNodes Map of nodeToCopyName--> copyNodeName
     * @param nodeTemplates
     * @param topology
     */
    private void processRelationships(Map<String, String> copiedNodes, Map<String, NodeTemplate> nodeTemplates, Topology topology) {
        WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology);
        copiedNodes.values().forEach(nodeName -> cleanRelationships(nodeName, copiedNodes, nodeTemplates, topologyContext));
        ;
    }

    private void copyNodeTemplate(NodeTemplate nodeTemplateToCopy, Map<String, String> copiedNodesNameMappings, Map<String, NodeTemplate> nodeTemplates,
            Topology topology) {
        // Build the new one
        NodeTemplate newNodeTemplate = CloneUtil.clone(nodeTemplateToCopy);
        newNodeTemplate.setName(copyName(nodeTemplateToCopy.getName(), nodeTemplates.keySet()));

        // load type
        NodeType type = ToscaContext.getOrFail(NodeType.class, nodeTemplateToCopy.getType());
        topologyService.loadType(topology, type);

        log.debug("Copying node template <{}>. Name is <{}> on the topology <{}> .", nodeTemplateToCopy.getName(), newNodeTemplate.getName(), topology.getId());
        // Put the new one in the topology
        nodeTemplates.put(newNodeTemplate.getName(), newNodeTemplate);

        // register the name mapping for further use
        copiedNodesNameMappings.put(nodeTemplateToCopy.getName(), newNodeTemplate.getName());

        // copy outputs
        copyOutputs(topology, nodeTemplateToCopy.getName(), newNodeTemplate.getName());

        WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology);

        // add the new node to the workflow
        workflowBuilderService.addNode(topologyContext, newNodeTemplate.getName(), newNodeTemplate);

        // copy hosted nodes
        safe(getHostedNodes(nodeTemplates, nodeTemplateToCopy.getName()))
                .forEach(nodeTemplate -> copyNodeTemplate(nodeTemplate, copiedNodesNameMappings, nodeTemplates, topology));

    }

    private List<NodeTemplate> getHostedNodes(Map<String, NodeTemplate> nodeTemplates, String nodeName) {
        return nodeTemplates.values().stream().filter(nodeTemplate -> safe(nodeTemplate.getRelationships()).values().stream()
                .anyMatch(relTemp -> relTemp.getTarget().equals(nodeName) && isHostedOn(relTemp.getType()))).collect(Collectors.toList());
    }

    /**
     * Discard all relationship targeting an "external" node. External here in terms of the hostedOn hierarchy
     * 
     * @param nodeName
     * @param validTargets A map of oldName -> copyName, we should keep relationships targeting one of these nodes.
     */
    private void cleanRelationships(String nodeName, Map<String, String> validTargets, Map<String, NodeTemplate> nodeTemplates,
            WorkflowsBuilderService.TopologyContext topologyContext) {

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

    private boolean isHostedOn(String type) {
        RelationshipType relationshipType = ToscaContext.getOrFail(RelationshipType.class, type);
        return ToscaNormativeUtil.isFromType(NormativeRelationshipConstants.HOSTED_ON, relationshipType);
    }

    private String copyName(String name, Collection<String> existingSet) {
        return TopologyUtils.getNexAvailableName(name + "_" + "copy", "", safe(existingSet));
    }

}
