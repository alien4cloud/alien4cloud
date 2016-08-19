package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.RemoveEdgeOperation;
import org.springframework.stereotype.Component;

import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.Workflow;
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
        log.debug("removing edge from <{}> to <{}> from the workflow <{}> from topology <{}>", operation.getFromStepId(), operation.getToStepId(),
                workflow.getName(), topology.getId());
        workflowBuilderService.removeEdge(topology, workflow.getName(), operation.getFromStepId(), operation.getToStepId());
    }
}
