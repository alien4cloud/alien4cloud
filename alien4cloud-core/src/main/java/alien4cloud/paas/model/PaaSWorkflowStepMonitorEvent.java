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
@Deprecated
public class PaaSWorkflowStepMonitorEvent extends AbstractPaaSWorkflowMonitorEvent {

    private String nodeId;
    private String instanceId;
    private String stepId;
    private String stage;
    private String operationName;

}