package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.RemoveEdgeOperation;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link RemoveEdgeOperation} operation
 */
@Slf4j
@Component
public class RemoveEdgeProcessor extends AbstractWorkflowProcessor<RemoveEdgeOperation> {

    @Override
    protected void processWorkflowOperation(RemoveEdgeOperation operation, Workflow workflow) {
        Topology topology = EditionContextManager.getTopology();
        log.debug("removing edge from [ {} ] to [ {} ] from the workflow [ {} ] from topology [ {} ]", operation.getFromStepId(), operation.getToStepId(),
                workflow.getName(), topology.getId());
        workflowBuilderService.removeEdge(topology, EditionContextManager.getCsar(), workflow.getName(), operation.getFromStepId(), operation.getToStepId());
    }
}
