package alien4cloud.paas.plan;

import java.util.List;

import lombok.Getter;

import com.google.common.collect.Lists;

/**
 * Run multiple steps in parallel.
 * 
 * @author luc boutier
 */
@Getter
public class ParallelGateway extends AbstractWorkflowStep {
    /** If closed then we should not add more steps but rather add the next one in next step. */
    private boolean closed = false;
    /** The steps to be performed in parallel. */
    private List<WorkflowStep> parallelSteps = Lists.newArrayList();
    private WorkflowStep lastInnerStep;

    public WorkflowStep addParallelStep(WorkflowStep step) {
        if(this.closed) {
            return this.setNextStep(step);
        }
        this.parallelSteps.add(step);
        return step;
    }
    
    public void setLastInnerStep(WorkflowStep lastInnerStep) {
        this.lastInnerStep = lastInnerStep;
    }
    
    public void close() {
        this.closed = true;
    }
}