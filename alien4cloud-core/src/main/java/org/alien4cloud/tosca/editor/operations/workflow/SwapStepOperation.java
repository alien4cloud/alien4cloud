package org.alien4cloud.tosca.editor.operations.workflow;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to swap a step with another one from a workflow
 */
@Getter
@Setter
public class SwapStepOperation extends AbstractWorkflowOperation {
    @NotBlank
    private String stepId;

    @NotBlank
    private String targetStepId;

    @Override
    public String commitMessage() {
        return "swap step <" + getStepId() + "> with <" + getTargetStepId() + "> from workflow <" + getWorkflowName() + ">";
    }
}
