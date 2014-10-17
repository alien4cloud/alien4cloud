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
    /** The steps to be performed in parallel. */
    private List<WorkflowStep> parallelSteps = Lists.newArrayList();

    public WorkflowStep addParallelStep(WorkflowStep step) {
        this.parallelSteps.add(step);
        return step;
    }
}