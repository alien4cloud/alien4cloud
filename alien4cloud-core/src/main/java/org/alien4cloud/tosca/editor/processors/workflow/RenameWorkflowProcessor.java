package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.RenameWorkflowOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.activities.InlineWorkflowActivity;
import org.springframework.stereotype.Component;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.paas.wf.util.WorkflowUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link RenameWorkflowOperation} operation
 * Renames a workflow
 */
@Slf4j
@Component
public class RenameWorkflowProcessor extends AbstractWorkflowProcessor<RenameWorkflowOperation> {

    @Override
    protected void processWorkflowOperation(Csar csar, Topology topology, RenameWorkflowOperation operation, Workflow workflow) {
        ensureNotStandard(workflow, "standard workflow <" + workflow.getName() + "> can not be renamed");
        String oldName = workflow.getName();
        WorkflowUtils.validateName(operation.getNewName());
        ensureUniqueness(topology, operation.getNewName());
        log.debug("renaming workflow [ {} ] to [ {} ] from topology [ {} ]", workflow.getName(), operation.getNewName(), topology.getId());
        topology.getWorkflows().remove(workflow.getName());
        workflow.setName(operation.getNewName());
        topology.getWorkflows().put(workflow.getName(), workflow);
        topology.getWorkflows().values().forEach(wf -> wf.getSteps().values().forEach(step -> {
            if (step.getActivity() instanceof InlineWorkflowActivity) {
                InlineWorkflowActivity inlineWorkflowActivity = (InlineWorkflowActivity) step.getActivity();
                if (inlineWorkflowActivity.getInline().equals(oldName)) {
                    inlineWorkflowActivity.setInline(operation.getNewName());
                }
            }
        }));
    }

    private void ensureUniqueness(Topology topology, String name) {
        if (topology.getWorkflows().containsKey(name)) {
            throw new AlreadyExistException(String.format("The workflow '%s' already exists in topology '%s'.", name, topology.getId()));
        }
    }
}
