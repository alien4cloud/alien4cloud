package org.alien4cloud.tosca.editor.processors.workflow;

import javax.inject.Inject;

import alien4cloud.paas.wf.TopologyContext;
import com.google.common.collect.Sets;
import org.alien4cloud.tosca.editor.operations.workflow.AbstractWorkflowOperation;
import org.alien4cloud.tosca.editor.operations.workflow.ReinitializeWorkflowOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;

import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.exception.BadWorkflowOperationException;

/**
 * Abstract processor to get a workflow.
 */
public abstract class AbstractWorkflowProcessor<T extends AbstractWorkflowOperation> implements IEditorOperationProcessor<T> {

    @Inject
    protected WorkflowsBuilderService workflowBuilderService;

    @Override
    public void process(Csar csar, Topology topology, T operation) {
        Workflow workflow = workflowBuilderService.getWorkflow(operation.getWorkflowName(), topology);
        processWorkflowOperation(csar, topology, operation, workflow);
        if (!operation.getClass().getSimpleName().toString().equals(ReinitializeWorkflowOperation.class.getSimpleName().toString())) {
            TopologyContext tc = workflowBuilderService.buildTopologyContext(topology,csar);
            workflow.setHasCustomModifications(true);
            workflowBuilderService.postProcessTopologyWorkflows(tc, Sets.newHashSet(operation.getWorkflowName()));
        }
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

    protected abstract void processWorkflowOperation(Csar csar, Topology topology, T operation, Workflow workflow);
}
