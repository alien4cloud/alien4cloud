package alien4cloud.paas.wf.util;

import alien4cloud.utils.jackson.MapEntry;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class WeightCaculatorTraversal extends AbstractGraphTraversal {

    protected WeightCaculatorTraversal(Workflow workflow, SubGraphFilter filter)  {
        super(workflow,filter);
    }

    private Map<String,Integer> weights = Maps.newHashMap();

    private Map<String, WorkflowStep> graph;

    @Override
    public void browse() {
        int weight = 1;

        graph = WorkflowGraphUtils.getAllStepsInSubGraph(workflow, filter);

        Map<String,Long> incidences = Maps.newHashMap();

        // Build incidence map
        for (WorkflowStep step : graph.values()) {
            incidences.put(step.getName(),step.getPrecedingSteps().stream().filter(s -> graph.containsKey(s)).count());
        }

        for (;;) {
            Set<String> steps = incidences.entrySet().stream().filter(e -> e.getValue() == 0).map(e -> e.getKey()).collect(Collectors.toSet());
            incidences.keySet().removeAll(steps);

            for (String stepName : steps)  {
                weights.put(stepName,weight);

                for (String childStepName : graph.get(stepName).getOnSuccess()) {
                    if (graph.containsKey(childStepName)) {
                        incidences.put(childStepName,incidences.get(childStepName)-1);
                    }
                }
            }

            if (incidences.size() == 0) break;

            weight++;
        }
    }

    public static WeightCaculatorTraversal builder(Workflow workflow, SubGraphFilter filter) {
        return new WeightCaculatorTraversal(workflow,filter);
    }

    public Map<String,WorkflowStep> getNodes() {
        return graph;
    }

    public Map<String,Integer> getWeights() {
        return weights;
    }
}
