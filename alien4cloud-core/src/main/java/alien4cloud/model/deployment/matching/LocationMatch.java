package alien4cloud.model.deployment.matching;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.model.orchestrators.locations.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains the result of a location matching.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationMatch implements ILocationMatch {
    /** Matched location */
    private Location location;
    /** Related orchestrator */
    private Orchestrator orchestrator;
    /** Reasons why this location is elected */
    private Object reasons;

    @Override
    public boolean isReady() {
        if (this.orchestrator == null) {
            return false;
        }
        return Objects.equals(orchestrator.getState(), OrchestratorState.CONNECTED);
    }
}
