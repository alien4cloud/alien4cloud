package alien4cloud.paas.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;

@Getter
@Setter
@ESObject
@ToString
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSDeploymentContext {
    /**
     * Id of an deployment (to manage the events)
     * For the same recipe, we might have multiple deployment ids.
     */
    private String deploymentId;
}
