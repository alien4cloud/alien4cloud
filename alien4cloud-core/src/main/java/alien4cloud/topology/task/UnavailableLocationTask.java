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
    private String locationName;
    private String orchestratorName;
}
