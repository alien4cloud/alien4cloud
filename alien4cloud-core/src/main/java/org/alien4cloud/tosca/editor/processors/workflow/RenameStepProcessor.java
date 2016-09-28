package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.RenameStepOperation;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.paas.wf.Workflow;
import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link RenameStepOperation} operation
 * Rename a workflow step
 */
@Slf4j
@Component
public class RenameStepProcessor extends AbstractWorkflowProcessor<RenameStepOperation> {

    @Override
    protected void processWorkflowOperation(RenameStepOperation operation, Workflow workflow) {
        Topology topology = EditionContextManager.getTopology();
        log.debug("renaming step <{}> to <{}> in workflow <{}> from topology <{}>", operation.getStepId(), operation.getNewName(), workflow.getName(),
                topology.getId());
        workflowBuilderService.renameStep(topology, workflow.getName(), operation.getStepId(), operation.getNewName());
    }
}
