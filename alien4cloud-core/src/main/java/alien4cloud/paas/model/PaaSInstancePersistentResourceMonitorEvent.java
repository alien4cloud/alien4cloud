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
@ToString(callSuper = true)
public class PaaSInstancePersistentResourceMonitorEvent extends PaaSInstanceStateMonitorEvent {
    /** The volumeId created / related to this instance **/
    private String propertyValue;
    private String propertyName;

    public PaaSInstancePersistentResourceMonitorEvent(String propertyName, String propertyValue) {
        super();
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    public PaaSInstancePersistentResourceMonitorEvent(PaaSInstanceStateMonitorEvent instanceStateMonitorEvent, String propertyName, String propertyValue) {
        super();
        setAttributes(instanceStateMonitorEvent.getAttributes());
        setRuntimeProperties(instanceStateMonitorEvent.getRuntimeProperties());
        setInstanceId(instanceStateMonitorEvent.getInstanceId());
        setNodeTemplateId(instanceStateMonitorEvent.getNodeTemplateId());
        setInstanceState(instanceStateMonitorEvent.getInstanceState());
        setInstanceStatus(instanceStateMonitorEvent.getInstanceStatus());
        setCloudId(instanceStateMonitorEvent.getCloudId());
        setDate(instanceStateMonitorEvent.getDate());
        setDeploymentId(instanceStateMonitorEvent.getDeploymentId());
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }
}