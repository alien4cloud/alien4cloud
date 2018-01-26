package alien4cloud.paas.wf.util;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.workflow.WorkflowStep;

public interface GraphConsumer {

    /**
     * When a node is reached when the graph is browsed the path from the root to it is returned
     *
     * @param path the path to reach the current node
     * @return false if the browse operation should be aborted, true otherwise
     */
    boolean onNewPath(List<WorkflowStep> path);

    void onAllNodes(Map<String, WorkflowStep> allNodes);
}
