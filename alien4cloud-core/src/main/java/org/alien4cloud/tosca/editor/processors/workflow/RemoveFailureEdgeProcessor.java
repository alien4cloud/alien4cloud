package org.alien4cloud.tosca.editor.processors.workflow;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.operations.workflow.RemoveEdgeOperation;
import org.alien4cloud.tosca.editor.operations.workflow.RemoveFailureEdgeOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.springframework.stereotype.Component;

/**
 * Process the {@link RemoveFailureEdgeOperation} operation
 */
@Slf4j
@Component
public class RemoveFailureEdgeProcessor extends AbstractWorkflowProcessor<RemoveFailureEdgeOperation>{
    @Override
    protected void processWorkflowOperation(Csar csar, Topology topology, RemoveFailureEdgeOperation operation, Workflow workflow) {
        log.debug("removing onFailure edge from [ {} ] to [ {} ] from the workflow [ {} ] from topology [ {} ]", operation.getFromStepId(), operation.getToStepId(),
                workflow.getName(), topology.getId());
        workflowBuilderService.removeFailureEdge(topology, csar, workflow.getName(), operation.getFromStepId(), operation.getToStepId());
    }
}
