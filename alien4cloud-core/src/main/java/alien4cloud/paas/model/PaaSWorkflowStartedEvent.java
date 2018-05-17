package alien4cloud.paas.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.ESObject;

/**
 * Should be fired when a workflow has been started.
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
@ToString
public class PaaSWorkflowStartedEvent extends AbstractPaaSWorkflowMonitorEvent {

    /**
     * The workflow name as it appears in a4c.
     */
    private String workflowName;

}