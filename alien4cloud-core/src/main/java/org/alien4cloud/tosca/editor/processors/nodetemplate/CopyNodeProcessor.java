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
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.ToscaNormativeUtil;
import org.alien4cloud.tosca.normative.constants.NormativeRelationshipConstants;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

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
 * Discard any other relationship.
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

        // Retrieve existing node template
        Map<String, NodeTemplate> nodeTemplates = TopologyUtils.getNodeTemplates(topology);
        NodeTemplate nodeTemplateToCopy = TopologyUtils.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);
        copyNodeTemplate(nodeTemplateToCopy, null, nodeTemplates, topology);

    }

    private void copyNodeTemplate(NodeTemplate nodeTemplateToCopy, NodeTemplate hostNodeTemplate, Map<String, NodeTemplate> nodeTemplates, Topology topology) {
        // Build the new one
        NodeTemplate newNodeTemplate = CloneUtil.clone(nodeTemplateToCopy);
        newNodeTemplate.setName(copyName(nodeTemplateToCopy.getName(), nodeTemplates.keySet()));

        if (hostNodeTemplate != null) {
            keepHostedOnRelationship(newNodeTemplate, hostNodeTemplate.getName());
        } else {
            newNodeTemplate.setRelationships(null);
        }

        // load type
        NodeType type = ToscaContext.getOrFail(NodeType.class, nodeTemplateToCopy.getType());
        topologyService.loadType(topology, type);

        log.debug("Copying node template <{}>. Name is <{}> on the topology <{}> .", nodeTemplateToCopy.getName(), newNodeTemplate.getName(), topology.getId());
        // Put the new one in the topology
        nodeTemplates.put(newNodeTemplate.getName(), newNodeTemplate);

        // copy outputs
        copyOutputs(topology, nodeTemplateToCopy.getName(), newNodeTemplate.getName());

        // TODO: check substitutions settings

        WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology);

        // add the new node to the workflow
        workflowBuilderService.addNode(topologyContext, newNodeTemplate.getName(), newNodeTemplate);
        // add remaining relationships to the workflow
        safe(newNodeTemplate.getRelationships()).values().forEach(
                relationshipTemplate -> workflowBuilderService.addRelationship(topologyContext, newNodeTemplate.getName(), relationshipTemplate.getName()));

        // copy hosted nodes
        safe(getHostedNodes(nodeTemplates, nodeTemplateToCopy.getName()))
                .forEach(nodeTemplate -> copyNodeTemplate(nodeTemplate, newNodeTemplate, nodeTemplates, topology));

    }

    private List<NodeTemplate> getHostedNodes(Map<String, NodeTemplate> nodeTemplates, String nodedName) {
        return nodeTemplates.values().stream().filter(nodeTemplate -> safe(nodeTemplate.getRelationships()).values().stream()
                .anyMatch(relTemp -> relTemp.getTarget().equals(nodedName) && isHostedOn(relTemp.getType()))).collect(Collectors.toList());
    }

    private void keepHostedOnRelationship(NodeTemplate nodeTemplate, String newTargetName) {
        if (MapUtils.isNotEmpty(nodeTemplate.getRelationships())) {
            Map<String, RelationshipTemplate> relationships = nodeTemplate.getRelationships();
            Set<String> keys = Sets.newHashSet(relationships.keySet());
            for (String key : keys) {
                RelationshipTemplate rel = relationships.remove(key);
                // if relationship is a hostedOn, then rename it, its target and keep it
                if (isHostedOn(rel.getType())) {
                    rel.setName(copyName(rel.getName(), relationships.keySet()));
                    rel.setTarget(newTargetName);
                    relationships.put(rel.getName(), rel);
                }
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
        copyValue(topology.getOutputCapabilityProperties(), toCopy, copyName);

        // output attributes
        copyValue(topology.getOutputAttributes(), toCopy, copyName);

        // substitution mapping
        if (topology.getSubstitutionMapping() != null) {
            if (topology.getSubstitutionMapping().getCapabilities() != null) {
                for (SubstitutionTarget st : topology.getSubstitutionMapping().getCapabilities().values()) {
                    if (st.getNodeTemplateName().equals(toCopy)) {
                        st.setNodeTemplateName(copyName);
                    }
                }
            }
            if (topology.getSubstitutionMapping().getRequirements() != null) {
                for (SubstitutionTarget st : topology.getSubstitutionMapping().getRequirements().values()) {
                    if (st.getNodeTemplateName().equals(toCopy)) {
                        st.setNodeTemplateName(copyName);
                    }
                }
            }
        }
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

    private boolean isHostedOn(String type) {
        RelationshipType relationshipType = ToscaContext.getOrFail(RelationshipType.class, type);
        return ToscaNormativeUtil.isFromType(NormativeRelationshipConstants.HOSTED_ON, relationshipType);
    }

    private String copyName(String name, Collection<String> existingSet) {
        return TopologyUtils.getNexAvailableName(name + "_" + "copy", "", safe(existingSet));
    }
}
