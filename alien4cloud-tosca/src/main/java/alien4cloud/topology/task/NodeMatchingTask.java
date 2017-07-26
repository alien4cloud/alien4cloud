package alien4cloud.topology.task;

import lombok.Getter;
import lombok.Setter;

/**
 * This task is triggered when no nodes are available on a location at matching phase to actually perform a replacement.
 */
@Getter
@Setter
public class NodeMatchingTask extends AbstractTask {
    public NodeMatchingTask() {
        setCode(TaskCode.NO_NODE_MATCHES);
    }
}