package org.alien4cloud.tosca.editor.operations.workflow;

import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to add an activity to a workflow
 */
@Getter
@Setter
public class AddActivityOperation extends AbstractWorkflowOperation {
    /**
     * If specified the step will be added near to this step.
     */
    private String relatedStepId;

    /**
     * If relatedStepId is specified, indicates if the step will be added before the related step (or after).
     */
    private boolean before;

    private AbstractWorkflowActivity activity;

    @Override
    public String commitMessage() {
        return "Add activity <" + getActivity().toString() + "> to workflow <" + getWorkflowName() + ">";
    }
}
