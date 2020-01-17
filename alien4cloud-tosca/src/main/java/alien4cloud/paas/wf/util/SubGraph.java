package alien4cloud.paas.wf.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;

import com.google.common.collect.Sets;

import lombok.Getter;

@Slf4j
@Getter
public class SubGraph {

    private Workflow workflow;

    private SubGraphFilter filter;

    public SubGraph(Workflow workflow, SubGraphFilter subGraphFilter) {
        this.workflow = workflow;
        this.filter = subGraphFilter;
    }

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

    @Override
    public String toString() {
        return "SubGraph{" + "workflow=" + workflow.getName() + ", filter=" + filter + '}';
    }
}
