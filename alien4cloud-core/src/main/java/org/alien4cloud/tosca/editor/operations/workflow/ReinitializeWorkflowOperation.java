package org.alien4cloud.tosca.editor.operations.workflow;

/**
 * Operation to reinitialize a new workflow
 */
public class ReinitializeWorkflowOperation extends AbstractWorkflowOperation {
    @Override
    public String commitMessage() {
        return "reinitialize workflow <" + getWorkflowName() + "> ";
    }
}
