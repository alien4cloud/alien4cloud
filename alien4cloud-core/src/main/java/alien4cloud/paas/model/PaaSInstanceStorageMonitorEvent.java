package alien4cloud.paas.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;

/**
 * Audit event related to an instance of a blockStorage node in a runtime topology.
 */
@Getter
@Setter
@ESObject
@NoArgsConstructor
@ToString(callSuper = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSInstanceStorageMonitorEvent extends PaaSInstanceStateMonitorEvent {
    /** The volumeId created / related to this instance **/
    private String volumeId;

    public PaaSInstanceStorageMonitorEvent(String volumeId) {
        super();
        this.volumeId = volumeId;
    }

    public PaaSInstanceStorageMonitorEvent(PaaSInstanceStateMonitorEvent instanceStateMonitorEvent, String volumeId) {
        super();
        setAttributes(instanceStateMonitorEvent.getAttributes());
        setProperties(instanceStateMonitorEvent.getProperties());
        setRuntimeProperties(instanceStateMonitorEvent.getRuntimeProperties());
        setInstanceId(instanceStateMonitorEvent.getInstanceId());
        setNodeTemplateId(instanceStateMonitorEvent.getNodeTemplateId());
        setInstanceState(instanceStateMonitorEvent.getInstanceState());
        setInstanceStatus(instanceStateMonitorEvent.getInstanceStatus());
        setCloudId(instanceStateMonitorEvent.getCloudId());
        setDate(instanceStateMonitorEvent.getDate());
        setDeploymentId(instanceStateMonitorEvent.getDeploymentId());
        setVolumeId(volumeId);
    }
}
