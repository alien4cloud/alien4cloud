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

    /** Indicates if the storage is deletable */
    private boolean isDeletable;

    public PaaSInstanceStorageMonitorEvent(String volumeId, boolean isDeletable) {
        super();
        this.volumeId = volumeId;
        this.isDeletable = isDeletable;
    }

    public PaaSInstanceStorageMonitorEvent(PaaSInstanceStateMonitorEvent instanceStateMonitorEvent, String volumeId, boolean isDeletable) {
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
        setVolumeId(volumeId);
        setDeletable(isDeletable);
    }
}
