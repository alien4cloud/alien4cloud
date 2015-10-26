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
public class PaaSInstancePersistentResourceMonitorEvent extends AbstractMonitorEvent {
    /** Id of the node template that describe the instance. */
    private String nodeTemplateId;
    /** Id of the instance within the node template group (for scalability purpose) */
    private String instanceId;
    /** The volumeId created / related to this instance **/
    private String propertyValue;
    private String propertyName;

    public PaaSInstancePersistentResourceMonitorEvent(String nodeTemplateId, String instanceId, String propertyName, String propertyValue) {
        this.nodeTemplateId = nodeTemplateId;
        this.instanceId = instanceId;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }
}