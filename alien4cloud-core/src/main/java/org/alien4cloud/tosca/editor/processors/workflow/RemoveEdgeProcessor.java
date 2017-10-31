package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.RemoveEdgeOperation;
import org.alien4cloud.tosca.model.Csar;
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
    protected void processWorkflowOperation(Csar csar, Topology topology, RemoveEdgeOperation operation, Workflow workflow) {
        log.debug("removing edge from [ {} ] to [ {} ] from the workflow [ {} ] from topology [ {} ]", operation.getFromStepId(), operation.getToStepId(),
                workflow.getName(), topology.getId());
        workflowBuilderService.removeEdge(topology, csar, workflow.getName(), operation.getFromStepId(), operation.getToStepId());
    }
}
