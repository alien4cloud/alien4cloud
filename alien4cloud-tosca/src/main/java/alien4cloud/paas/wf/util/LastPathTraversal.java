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

    /*
    public void browse(GraphConsumer graphConsumer) {
        Map<String, WorkflowStep> subGraphSteps = WorkflowGraphUtils.getAllStepsInSubGraph(workflow, filter);
        Set<String> allSubGraphNodeIds = subGraphSteps.keySet();
        List<WorkflowStep> rootNodes = subGraphSteps.values().stream().filter(node -> Collections.disjoint(node.getPrecedingSteps(), allSubGraphNodeIds))
                .collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            log.debug("Graph has {} root nodes", rootNodes.size());
            for (WorkflowStep step : rootNodes) {
                log.debug("\t- {}", step.getName());
            }
        }
        if (rootNodes.isEmpty() && !subGraphSteps.isEmpty()) {
            // It means the whole sub graph is connected between them, we begin anyway with one of the node
            rootNodes.add(subGraphSteps.values().iterator().next());
        }

        Map<String, WorkflowStep> allNodes = new HashMap<>();
        for (WorkflowStep rootNode : rootNodes) {
            if (log.isDebugEnabled()) {
                log.debug("Analysing subgraph starting from {}", rootNode.getName());
            }
            boolean shouldContinue = internalBrowseSubGraph(subGraphSteps, graphConsumer, new ArrayList<>(), rootNode, allNodes);
            if (!shouldContinue) {
                break;
            }
        }
        graphConsumer.onAllNodes(allNodes);
    }

    private boolean internalBrowseSubGraph(Map<String, WorkflowStep> subGraphSteps, GraphConsumer graphConsumer, List<WorkflowStep> parentPath,
                                           WorkflowStep currentNode, Map<String, WorkflowStep> allNodes) {
        allNodes.put(currentNode.getName(), currentNode);
        List<WorkflowStep> newPath = new ArrayList<>(parentPath);
        newPath.add(currentNode);

        StringBuilder builder = new StringBuilder();
        for (WorkflowStep step : newPath) {
            builder.append("/");
            builder.append(step.getName());
        }

        if (log.isTraceEnabled()) {
            log.trace("Adding node {} to a path containing {} nodes", currentNode.getName(), newPath.size());
        }
        boolean shouldContinue = graphConsumer.onNewPath(newPath);
        if (!shouldContinue) {
            return false;
        }
        Set<String> childrenIds = Sets.intersection(currentNode.getOnSuccess(), subGraphSteps.keySet());
        for (String childNodeId : childrenIds) {
            shouldContinue = internalBrowseSubGraph(subGraphSteps, graphConsumer, newPath, subGraphSteps.get(childNodeId), allNodes);
            if (!shouldContinue) {
                return false;
            }
        }
        return true;
    }
*/


}
