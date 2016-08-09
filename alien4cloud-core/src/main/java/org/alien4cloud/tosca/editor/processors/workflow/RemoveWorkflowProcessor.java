package org.alien4cloud.tosca.editor.processors.workflow;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.RemoveWorkflowOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link RemoveWorkflowOperation} operation
 * Removes a workflow.
 */
@Slf4j
@Component
public class RemoveWorkflowProcessor extends AbstractWorkflowProcessor<RemoveWorkflowOperation> {

    @Override
    protected void processWorkflowOperation(RemoveWorkflowOperation operation, Workflow workflow) {
        Topology topology = EditionContextManager.getTopology();
        ensureNotStandard(workflow, "standard workflow <" + workflow.getName() + "> can not be removed");
        log.debug("removing workflow <{}> from topology <{}>", operation.getWorkflowName(), topology.getId());
        topology.getWorkflows().remove(operation.getWorkflowName());
    }

}
