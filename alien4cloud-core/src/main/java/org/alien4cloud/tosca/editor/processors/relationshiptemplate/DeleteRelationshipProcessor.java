package org.alien4cloud.tosca.editor.processors.relationshiptemplate;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.DeleteRelationshipOperation;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Process a delete relationship operation.
 */
@Slf4j
@Component
public class DeleteRelationshipProcessor extends AbstractRelationshipProcessor<DeleteRelationshipOperation> {
    @Resource
    private TopologyService topologyService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    protected void processRelationshipOperation(DeleteRelationshipOperation operation, NodeTemplate nodeTemplate, RelationshipTemplate relationshipTemplate) {
        Topology topology = EditionContextManager.getTopology();
        log.debug("Removing the Relationship template <" + operation.getRelationshipName() + "> from the Node template <" + operation.getNodeName()
                + ">, Topology <" + topology.getId() + "> .");
        topologyService.unloadType(topology, relationshipTemplate.getType());
        nodeTemplate.getRelationships().remove(operation.getRelationshipName());
        workflowBuilderService.removeRelationship(topology, operation.getNodeName(), operation.getRelationshipName(), relationshipTemplate);
    }
}