package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.nodetemplate.ReplaceNodeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import lombok.extern.slf4j.Slf4j;

/**
 * Replace the type of a node template by another compatible type (inherited or that fulfills the same used capabilities and requirements).
 */
@Slf4j
@Component
public class ReplaceNodeProcessor implements IEditorOperationProcessor<ReplaceNodeOperation> {
    @Inject
    private IToscaTypeSearchService searchService;
    @Inject
    private TopologyService topologyService;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private ICsarService csarService;

    @Override
    public void process(ReplaceNodeOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        Set<CSARDependency> oldDependencies = topology.getDependencies();
        // Retrieve existing node template
        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
        NodeTemplate oldNodeTemplate = TopologyServiceCore.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);

        String[] splittedId = operation.getNewTypeId().split(":");
        NodeType newType = searchService.find(NodeType.class, splittedId[0], splittedId[1]);
        // Load the new type to the topology in order to update its dependencies
        newType = topologyService.loadType(topology, newType);

        // FIXME we should clone all possible properties, capabilities properties, requirements properties and relationships
        // TODO we should check compatibility between the old and new types to do so
        // Build the new one
        NodeTemplate newNodeTemplate = topologyService.buildNodeTemplate(topology.getDependencies(), newType, null);
        newNodeTemplate.setName(oldNodeTemplate.getName());
        newNodeTemplate.setRelationships(oldNodeTemplate.getRelationships());
        // Put the new one in the topology
        nodeTemplates.put(oldNodeTemplate.getName(), newNodeTemplate);

        // Unload and remove old node template
        topologyService.unloadType(topology, oldNodeTemplate.getType());
        // remove the node from the workflows
        workflowBuilderService.removeNode(topology, oldNodeTemplate.getName(), oldNodeTemplate);

        // FIXME we should remove outputs/inputs, others here ?
        if (topology.getSubstitutionMapping() != null) {
            removeNodeTemplateSubstitutionTargetMapEntry(oldNodeTemplate.getName(), topology.getSubstitutionMapping().getCapabilities());
            removeNodeTemplateSubstitutionTargetMapEntry(oldNodeTemplate.getName(), topology.getSubstitutionMapping().getRequirements());
        }

        log.debug("Replacing the node template<{}> with <{}> bound to the node type <{}> on the topology <{}> .", oldNodeTemplate.getName(),
                oldNodeTemplate.getName(), operation.getNewTypeId(), topology.getId());

        // add the new node to the workflow
        workflowBuilderService.addNode(workflowBuilderService.buildTopologyContext(topology), oldNodeTemplate.getName(), newNodeTemplate);
        // If dependencies changed then must update also CSAR dependencies
        if (!Objects.equals(topology.getDependencies(), oldDependencies)) {
            csarService.setDependencies(topology.getId(), topology.getDependencies());
        }
    }

    private void removeNodeTemplateSubstitutionTargetMapEntry(String nodeTemplateName, Map<String, SubstitutionTarget> substitutionTargets) {
        if (substitutionTargets == null) {
            return;
        }
        Iterator<Map.Entry<String, SubstitutionTarget>> capabilities = substitutionTargets.entrySet().iterator();
        while (capabilities.hasNext()) {
            Map.Entry<String, SubstitutionTarget> e = capabilities.next();
            if (e.getValue().getNodeTemplateName().equals(nodeTemplateName)) {
                capabilities.remove();
            }
        }
    }
}
