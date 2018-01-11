package alien4cloud.paas.wf.validation;

import java.util.ArrayList;
import java.util.List;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.elasticsearch.common.collect.Lists;

import alien4cloud.paas.wf.model.Path;
import alien4cloud.paas.wf.TopologyContext;
import alien4cloud.paas.wf.util.WorkflowGraphUtils;

/**
 * A cycle in the workflow is not permit.
 * <p>
 * 
 */
public class CycleDetection implements Rule {

    @Override
    public List<AbstractWorkflowError> validate(TopologyContext topologyContext, Workflow workflow) {
        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            return null;
        }
        List<AbstractWorkflowError> result = Lists.newArrayList();
        // get all the paths
        List<Path> paths = WorkflowGraphUtils.getWorkflowGraphCycles(workflow);
        for (Path path : paths) {
            // isolate cycles
            result.add(new WorkflowHasCycleError(extractCycle(path.getStepNames())));
        }
        return result;
    }

    private List<String> extractCycle(List<String> path) {
        List<String> cycle = new ArrayList<String>();
        // this is in fact the step that generate the cycle
        // it's present 2 times in the path
        String incriminedStep = path.get(path.size() - 1);
        boolean detected = false;
        for (String step : path) {
            // iterate unil we find the first occurence of incriminedStep
            if (!detected && step.equals(incriminedStep)) {
                detected = true;
            }
            if (detected) {
                cycle.add(step);
            }
        }
        return cycle;
    }

}
