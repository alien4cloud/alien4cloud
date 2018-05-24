package alien4cloud.paas.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.ESObject;

/**
 * This event should be triggered when a workflow step instance is about to be considered.
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
@ToString
public abstract class AbstractWorkflowStepEvent extends AbstractPaaSWorkflowMonitorEvent {

    private String nodeId;
    private String instanceId;
    private String stepId;
    private String operationName;
    private String targetNodeId;
    private String targetInstanceId;

}