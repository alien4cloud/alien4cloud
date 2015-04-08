package alien4cloud.paas.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class InstanceInformation {

    /**
     * The textual representation of the state of the instance.
     */
    private String state;

    /**
     * The effective representation of the state of the instance (SUCCESS, PROCESSING, FAILURE).
     */
    private InstanceStatus instanceStatus;

    /** Values of attributes for this instance. */
    private Map<String, String> attributes;
    /** Additional properties specific from the container. */
    private Map<String, String> runtimeProperties;
}
