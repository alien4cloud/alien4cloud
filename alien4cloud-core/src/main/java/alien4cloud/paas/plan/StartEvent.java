package alien4cloud.paas.plan;

/**
 * The initial step of a workflow.
 * 
 * @author luc boutier
 */
public class StartEvent extends AbstractWorkflowStep {
    @Override
    public void setPreviousStep(WorkflowStep previousStep) {
        throw new IllegalStateException("Cannot add a previous step to a start event.");
    }

    @Override
    public WorkflowStep getPreviousStep() {
        return null;
    }
}