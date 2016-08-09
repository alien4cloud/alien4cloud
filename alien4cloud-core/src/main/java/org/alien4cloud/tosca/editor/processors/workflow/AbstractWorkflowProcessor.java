package org.alien4cloud.tosca.editor.processors.workflow;

import javax.inject.Inject;

import alien4cloud.paas.wf.exception.BadWorkflowOperationException;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.AbstractWorkflowOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;

import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService;

/**
 * Abstract processor to get a workflow.
 */
public abstract class AbstractWorkflowProcessor<T extends AbstractWorkflowOperation> implements IEditorOperationProcessor<T> {

    @Inject
    protected WorkflowsBuilderService workflowBuilderService;

    @Override
    public void process(T operation) {
        Topology topology = EditionContextManager.getTopology();
        Workflow workflow = workflowBuilderService.getWorkflow(operation.getWorkflowName(), topology);
        processWorkflowOperation(operation, workflow);
    }

    /**
     * Ensure we are <em><b>NOT</b></em> working with a Standard workflow.<br/>
     * This might be useful, for example, for operations like renaming or removing that are allowed on non standard workflow
     * 
     * @param workflow
     */
    protected void ensureNotStandard(Workflow workflow, String message) {
        if (workflow.isStandard()) {
            throw new BadWorkflowOperationException(message);
        }
    }

    /**
     * Ensure we are working with a Standard workflow.<br/>
     * This might be usefull, for example, for operations like reinitializing that are allowed only on standard
     * workflow
     * 
     * @param workflow
     * @param message Message for the exception to throw
     */
    protected void ensureStandard(Workflow workflow, String message) {
        if (!workflow.isStandard()) {
            throw new BadWorkflowOperationException(message);
        }
    }

    protected abstract void processWorkflowOperation(T operation, Workflow workflow);
}
