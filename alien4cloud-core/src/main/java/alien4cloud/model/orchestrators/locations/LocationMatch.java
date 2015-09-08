package alien4cloud.model.orchestrators.locations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.orchestrators.Orchestrator;

/**
 * Location matched given a topology
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationMatch {
    /** Matched location */
    private Location location;
    /** related orchestrator */
    private Orchestrator orchestrator;
    /** Reasons why this location is elected */
    private Object reasons;
}
