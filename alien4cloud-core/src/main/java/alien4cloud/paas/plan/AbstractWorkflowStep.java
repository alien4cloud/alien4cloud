package alien4cloud.paas.plan;

import lombok.Getter;

@Getter
@SuppressWarnings("PMD.UnusedPrivateField")
public abstract class AbstractWorkflowStep implements WorkflowStep {
    /** The next step of in workflow. */
    private WorkflowStep previousStep;
    /** The next step of a workflow. */
    private WorkflowStep nextStep;

    public void setPreviousStep(WorkflowStep previousStep) {
        this.previousStep = previousStep;
    }

    @Override
    public WorkflowStep setNextStep(WorkflowStep step) {
        this.nextStep = step;
        return step;
    }
}