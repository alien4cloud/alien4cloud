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
// TODO: rename : TaskScheduledEvent
public class TaskSentEvent extends AbstractTaskEvent {

}