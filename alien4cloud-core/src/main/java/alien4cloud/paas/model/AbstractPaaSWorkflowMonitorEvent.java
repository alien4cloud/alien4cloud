package alien4cloud.paas.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;

/**
 * This event allows the PaaS Provider to update the deployment topology of an application so that persistent resources created at deployment time will be
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
@ToString
public abstract class AbstractPaaSWorkflowMonitorEvent extends AbstractMonitorEvent {

    private String workflowId;
    private String executionId;

}