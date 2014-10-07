package alien4cloud.paas.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;

/**
 * Audit event related to the state of an instance of a node in a runtime topology.
 */
@Getter
@Setter
@ESObject
@ToString(callSuper = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSInstanceStateMonitorEvent extends AbstractMonitorEvent {
    /** Id of the node template that describe the instance. */
    private String nodeTemplateId;
    /** Id of the instance within the node template group (for scalability purpose) */
    private String instanceId;
    /** State of the instance. */
    private String instanceState;
    /** The status of the instance */
    private InstanceStatus instanceStatus;
    /** The properties of the instance **/
    private Map<String, String> attributes;

    private Map<String, String> properties;

    private Map<String, String> runtimeProperties;
}
