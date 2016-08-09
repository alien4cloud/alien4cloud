package org.alien4cloud.tosca.editor.operations.workflow;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to rename an existing step within a workflow
 */
@Getter
@Setter
public class RenameStepOperation extends AbstractWorkflowOperation {

    @NotBlank
    private String stepId;

    /* new name to assign */
    @NotBlank
    private String newName;

    @Override
    public String commitMessage() {
        return "rename step <" + getStepId() + "> to <" + getNewName() + "> in workflow <" + getWorkflowName() + "> ";
    }
}
