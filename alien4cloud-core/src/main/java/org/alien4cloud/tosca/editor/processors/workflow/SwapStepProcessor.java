package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.SwapStepOperation;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link SwapStepOperation} operation
 * Swaps two workflow steps
 */
@Slf4j
@Component
public class SwapStepProcessor extends AbstractWorkflowProcessor<SwapStepOperation> {

    @Override
    protected void processWorkflowOperation(SwapStepOperation operation, Workflow workflow) {
        Topology topology = EditionContextManager.getTopology();
        log.debug("swapping step [ {} ] with [ {} ] in workflow [ {} ] from topology [ {} ]", operation.getStepId(), operation.getTargetStepId(), workflow.getName(),
                topology.getId());
        workflowBuilderService.swapSteps(topology, EditionContextManager.getCsar(), workflow.getName(), operation.getStepId(), operation.getTargetStepId());
    }
}
