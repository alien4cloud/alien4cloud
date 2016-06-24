package org.alien4cloud.tosca.editor.processors.relationshiptemplate;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.TopologyEditionContextManager;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.DeleteRelationshipOperation;
import org.alien4cloud.tosca.editor.processors.nodetemplate.AbstractNodeProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
@Component
public class DeleteRelationshipProcessor extends AbstractNodeProcessor<DeleteRelationshipOperation> {
    @Resource
    private TopologyService topologyService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    protected void processNodeOperation(DeleteRelationshipOperation operation, NodeTemplate template) {
        Topology topology = TopologyEditionContextManager.getTopology();
        log.debug("Removing the Relationship template <" + operation.getRelationshipName() + "> from the Node template <" + operation.getNodeName()
                + ">, Topology <" + topology.getId() + "> .");
        RelationshipTemplate relationshipTemplate = template.getRelationships().get(operation.getRelationshipName());
        if (relationshipTemplate != null) {
            topologyService.unloadType(topology, relationshipTemplate.getType());
            template.getRelationships().remove(operation.getRelationshipName());
        } else {
            throw new NotFoundException("The relationship with name [" + operation.getRelationshipName() + "] do not exist for the node ["
                    + operation.getNodeName() + "] of the topology [" + topology.getId() + "]");
        }
        workflowBuilderService.removeRelationship(topology, operation.getNodeName(), operation.getRelationshipName(), relationshipTemplate);
    }
}