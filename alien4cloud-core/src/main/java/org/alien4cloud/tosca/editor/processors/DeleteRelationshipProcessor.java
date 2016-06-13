package org.alien4cloud.tosca.editor.processors;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.TopologyEditionContextManager;
import org.alien4cloud.tosca.editor.commands.DeleteRelationshipOperation;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
@Component
public class DeleteRelationshipProcessor implements IEditorOperationProcessor<DeleteRelationshipOperation> {
    @Resource
    private TopologyService topologyService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    public void process(DeleteRelationshipOperation operation) {
        Topology topology = TopologyEditionContextManager.getTopology();

        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);

        NodeTemplate template = TopologyServiceCore.getNodeTemplate(topology.getId(), operation.getNodeTemplateName(), nodeTemplates);
        log.debug("Removing the Relationship template <" + operation.getRelationshipTemplateName() + "> from the Node template <"
                + operation.getNodeTemplateName() + ">, Topology <" + topology.getId() + "> .");
        RelationshipTemplate relationshipTemplate = template.getRelationships().get(operation.getRelationshipTemplateName());
        if (relationshipTemplate != null) {
            topologyService.unloadType(topology, relationshipTemplate.getType());
            template.getRelationships().remove(operation.getRelationshipTemplateName());
        } else {
            throw new NotFoundException("The relationship with name [" + operation.getRelationshipTemplateName() + "] do not exist for the node ["
                    + operation.getNodeTemplateName() + "] of the topology [" + topology.getId() + "]");
        }
        workflowBuilderService.removeRelationship(topology, operation.getNodeTemplateName(), operation.getRelationshipTemplateName(), relationshipTemplate);
    }
}
