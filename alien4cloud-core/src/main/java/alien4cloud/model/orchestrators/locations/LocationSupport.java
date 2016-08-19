package alien4cloud.model.orchestrators.locations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains information on location support by an orchestrator.
 */
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@Getter
@Setter
public class LocationSupport {
    /** True if the orchestrator supports multiple locations, false if a single location is supported. */
    private boolean multipleLocations;
    /** List of location types supported by the orchestrator. */
    private String[] types;
}