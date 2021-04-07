package alien4cloud.paas.wf.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;

import java.util.*;
import java.util.stream.Collectors;

public class LastPathTraversal extends AbstractGraphTraversal  {
    protected LastPathTraversal(Workflow workflow, SubGraphFilter filter)  {
        super(workflow,filter);
    }

    private Map<String, WorkflowStep> graph;

    private List<WorkflowStep> path = Lists.newLinkedList();

    @Override
    public void browse() {
        graph = WorkflowGraphUtils.getAllStepsInSubGraph(workflow, filter);

        // Get Nodes with no precedence in this graph
        List<WorkflowStep> nodes = graph.values().stream().filter(node -> Collections.disjoint(node.getPrecedingSteps(), graph.keySet())).collect(Collectors.toList());

        if (nodes.isEmpty() && !graph.isEmpty()) {
            // It means the whole sub graph is connected between them, we begin anyway with one of the node
            nodes.add(graph.values().iterator().next());
        }

        while(nodes.size() > 0) {
            WorkflowStep step = nodes.get(nodes.size() - 1);

            path.add(step);

            nodes = Lists.newLinkedList();

            for (String stepName : step.getOnSuccess()) {
                if (graph.containsKey(stepName)) {
                    nodes.add(graph.get(stepName));
                }
            }
        }
    }

    public static LastPathTraversal builder(Workflow workflow, SubGraphFilter filter) {
        return new LastPathTraversal(workflow,filter);
    }

    public Map<String,WorkflowStep> getNodes() {
        return graph;
    }

    public List<WorkflowStep> getPath() {
        return path;
    }

}
