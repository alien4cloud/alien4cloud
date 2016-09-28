package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.RemoveStepOperation;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.paas.wf.Workflow;
import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link RemoveStepOperation} operation
 * Removes a workflow.
 */
@Slf4j
@Component
public class RemoveStepProcessor extends AbstractWorkflowProcessor<RemoveStepOperation> {

    @Override
    protected void processWorkflowOperation(RemoveStepOperation operation, Workflow workflow) {
        Topology topology = EditionContextManager.getTopology();
        log.debug("removing step <{}> from workflow <{}> from topology <{}>", operation.getStepId(), workflow.getName(), topology.getId());
        workflowBuilderService.removeStep(topology, workflow.getName(), operation.getStepId(), false);
    }

}
