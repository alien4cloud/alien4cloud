package alien4cloud.paas.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.ESObject;

/**
 * This event should be triggered when a workflow step instance has been started.
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
@ToString
public class WorkflowStepStartedEvent extends AbstractWorkflowStepEvent {

}