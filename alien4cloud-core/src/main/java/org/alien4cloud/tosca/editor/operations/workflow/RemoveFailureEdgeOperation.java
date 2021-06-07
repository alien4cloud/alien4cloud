package org.alien4cloud.tosca.editor.operations.workflow;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Operation to remove an onFailure edge (link between two steps) from a workflow
 */
@Getter
@Setter
public class RemoveFailureEdgeOperation extends AbstractWorkflowOperation {
    /**
     * remove from this step to {@link RemoveFailureEdgeOperation#toStepId}.
     */
    @NotBlank
    private String fromStepId;
    /**
     * remove from {@link RemoveFailureEdgeOperation#fromStepId} to this step
     */
    @NotBlank
    private String toStepId;

    @Override
    public String commitMessage() {
        return "Remove onFailure edge from step <" + getFromStepId() + "> to step <" + getToStepId() + "> from  the workflow <" + getWorkflowName() + ">";
    }
}
