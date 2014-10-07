package alien4cloud.paas.plan;

/**
 * A specific {@link WorkflowStep} that cannot have any next step..
 * 
 * @author luc boutier
 */
public class StopEvent extends AbstractWorkflowStep {
    @Override
    public WorkflowStep setNextStep(WorkflowStep step) {
        throw new IllegalStateException("Cannot add a next step to a stop event.");
    }

    @Override
    public WorkflowStep getNextStep() {
        return null;
    }
}