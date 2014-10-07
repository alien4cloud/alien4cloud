package alien4cloud.paas.plan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * A step in the workflow.
 * 
 * @author luc boutier
 */
@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "type")
public interface WorkflowStep {
    /**
     * <p>
     * Set (in case the current step supports only a single next step) or add a step to be the current's step next step.
     * </p>
     * 
     * @param step The step to be added next to the current one in the workflow.
     * @return The step we just added.
     */
    WorkflowStep setNextStep(WorkflowStep step);

    /**
     * <p>
     * Get the next step of the workflow.
     * </p>
     * 
     * @return The next step of the workflow.
     * */
    WorkflowStep getNextStep();

    /**
     * Step that has been executed synchronously before the current step.
     * 
     * @return The step that must be executed synchronously before the current step.
     */
    @JsonIgnore
    WorkflowStep getPreviousStep();

    /**
     * Set the step executed synchronously before the current step
     * 
     * @param previousStep The step that must be executed synchronously before the current step.
     */
    void setPreviousStep(WorkflowStep previousStep);
}