package org.alien4cloud.tosca.editor.operations.workflow;

/**
 * Operation to remove an existing workflow
 */
public class RemoveWorkflowOperation extends AbstractWorkflowOperation {
    @Override
    public String commitMessage() {
        return "remove workflow <" + getWorkflowName() + ">";
    }
}
