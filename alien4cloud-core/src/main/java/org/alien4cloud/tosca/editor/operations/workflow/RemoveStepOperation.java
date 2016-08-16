package org.alien4cloud.tosca.editor.operations.workflow;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Operation to remove an existing step from a workflow
 */
@Getter
@Setter
public class RemoveStepOperation extends AbstractWorkflowOperation {
    @NotBlank
    private String stepId;

    @Override
    public String commitMessage() {
        return "remove step <" + getStepId() + "> from workflow <" + getWorkflowName() + ">";
    }
}
