package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.RemoveWorkflowOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.activities.InlineWorkflowActivity;
import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.exception.BadWorkflowOperationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link RemoveWorkflowOperation} operation
 * Removes a workflow.
 */
@Slf4j
@Component
public class RemoveWorkflowProcessor extends AbstractWorkflowProcessor<RemoveWorkflowOperation> {

    @Override
    protected void processWorkflowOperation(Csar csar, Topology topology, RemoveWorkflowOperation operation, Workflow workflow) {
        ensureNotStandard(workflow, "standard workflow <" + workflow.getName() + "> can not be removed");
        log.debug("removing workflow [ {} ] from topology [ {} ]", operation.getWorkflowName(), topology.getId());
        topology.getWorkflows().remove(operation.getWorkflowName());
        topology.getWorkflows().values().forEach(wf -> wf.getSteps().values().forEach(step -> {
            if (step.getActivity() instanceof InlineWorkflowActivity) {
                InlineWorkflowActivity inlineWorkflowActivity = (InlineWorkflowActivity) step.getActivity();
                if (inlineWorkflowActivity.getInline().equals(workflow.getName())) {
                    throw new BadWorkflowOperationException(
                            "Workflow " + inlineWorkflowActivity.getInline() + " is inlined in workflow " + wf.getName() + " in step " + step.getName());
                }
            }
        }));
    }

}
