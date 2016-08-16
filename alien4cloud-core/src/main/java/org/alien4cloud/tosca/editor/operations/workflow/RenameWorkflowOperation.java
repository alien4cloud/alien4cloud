package org.alien4cloud.tosca.editor.operations.workflow;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Operation to rename an existing workflow
 */
@Getter
@Setter
public class RenameWorkflowOperation extends AbstractWorkflowOperation {
    /* new name to assign */
    @NotBlank
    private String newName;

    @Override
    public String commitMessage() {
        return "rename workflow <" + getWorkflowName() + "> to <" + getNewName() + ">";
    }
}
