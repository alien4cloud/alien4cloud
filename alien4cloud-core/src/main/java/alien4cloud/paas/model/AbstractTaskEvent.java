package alien4cloud.paas.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.ESObject;

/**
 * An event related to a task.
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
@ToString
public abstract class AbstractTaskEvent extends AbstractPaaSWorkflowMonitorEvent {

    /**
     * ID of the task is provided by the orchestrator.
     */
    private String taskId;

    private String nodeId;
    private String instanceId;

    // if the task concerns a relationship, then target informations should be filled
    private String targetNodeId;
    private String targetInstanceId;

    private String operationName;
    private String workflowStepId;
}