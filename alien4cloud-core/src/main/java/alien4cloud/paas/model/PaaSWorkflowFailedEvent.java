package alien4cloud.paas.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.ESObject;

/**
 * Should be fired when a workflow has failed.
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
@ToString
public class PaaSWorkflowFailedEvent extends PaaSWorkflowFinishedEvent {


}