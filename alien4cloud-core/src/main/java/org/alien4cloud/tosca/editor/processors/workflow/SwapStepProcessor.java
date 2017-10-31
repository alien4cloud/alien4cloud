package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.SwapStepOperation;
import org.alien4cloud.tosca.model.Csar;
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
    protected void processWorkflowOperation(Csar csar, Topology topology, SwapStepOperation operation, Workflow workflow) {
        log.debug("swapping step [ {} ] with [ {} ] in workflow [ {} ] from topology [ {} ]", operation.getStepId(), operation.getTargetStepId(), workflow.getName(),
                topology.getId());
        workflowBuilderService.swapSteps(topology, csar, workflow.getName(), operation.getStepId(), operation.getTargetStepId());
    }
}
