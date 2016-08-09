package org.alien4cloud.tosca.editor.operations.workflow;

/**
 * Operation to create a new workflow
 */
public class CreateWorkflowOperation extends AbstractWorkflowOperation {
    @Override
    public String commitMessage() {
        return "create workflow <" + getWorkflowName() + "> ";
    }
}
