package alien4cloud.model.deployment.matching;

import alien4cloud.model.orchestrators.locations.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.orchestrators.Orchestrator;

/**
 * Contains the result of a location matching.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationMatch {
    /** Matched location */
    private Location location;
    /** Related orchestrator */
    private Orchestrator orchestrator;
    /** Reasons why this location is elected */
    private Object reasons;
}
