package alien4cloud.paas.model;

import lombok.*;

import org.elasticsearch.annotation.ESObject;

/**
 * This event allows the PaaS Provider to update the deployment topology of an application so that persistent resources created at deployment time will be
 */
@Getter
@Setter
@ESObject
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
@ToString
public class PaaSInstancePersistentResourceMonitorEvent extends AbstractMonitorEvent {
    /** Id of the node template that describe the instance. */
    private String nodeTemplateId;
    /** Id of the instance within the node template group (for scalability purpose) */
    private String instanceId;
    private String propertyName;
    /** The volumeId created / related to this instance **/
    private Object propertyValue;
}