package alien4cloud.topology.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * Location policy task
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class UnavailableLocationTask extends LocationPolicyTask {
    // Name of the node template that needs to be fixed.
    private String locationId;
    private String orchestratorId;
    private String reason;
}
