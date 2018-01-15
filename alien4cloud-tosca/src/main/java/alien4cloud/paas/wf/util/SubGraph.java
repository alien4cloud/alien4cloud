package alien4cloud.paas.wf.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;

import com.google.common.collect.Sets;

import lombok.Getter;

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
        if (rootNodes.isEmpty() && !subGraphSteps.isEmpty()) {
            // It means the whole sub graph is connected between them, we begin anyway with one of the node
            rootNodes.add(subGraphSteps.values().iterator().next());
        }
        List<Map<String, WorkflowStep>> components = new ArrayList<>();
        for (WorkflowStep rootNode : rootNodes) {
            Map<String, WorkflowStep> component = new LinkedHashMap<>();
            components.add(component);
            boolean shouldContinue = internalBrowseSubGraph(subGraphSteps, graphConsumer, new ArrayList<>(), rootNode, component);
            if (!shouldContinue) {
                break;
            }
        }
        // FIXME this is not a definition of component of a graph, for the moment the configured workflow does not have components so not very important to fix
        graphConsumer.onRoots(components);
    }

    private boolean internalBrowseSubGraph(Map<String, WorkflowStep> subGraphSteps, GraphConsumer graphConsumer, List<WorkflowStep> parentPath,
            WorkflowStep currentNode, Map<String, WorkflowStep> component) {
        component.put(currentNode.getName(), currentNode);
        List<WorkflowStep> newPath = new ArrayList<>(parentPath);
        newPath.add(currentNode);
        boolean shouldContinue = graphConsumer.onNewPath(newPath);
        if (!shouldContinue) {
            return false;
        }
        Set<String> childrenIds = Sets.intersection(currentNode.getOnSuccess(), subGraphSteps.keySet());
        for (String childNodeId : childrenIds) {
            shouldContinue = internalBrowseSubGraph(subGraphSteps, graphConsumer, newPath, subGraphSteps.get(childNodeId), component);
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
