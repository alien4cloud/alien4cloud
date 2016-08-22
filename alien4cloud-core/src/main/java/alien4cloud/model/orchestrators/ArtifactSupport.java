package alien4cloud.model.orchestrators;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains information on artifacts supported by an orchestrator.
 */
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@Getter
@Setter
public class ArtifactSupport {
    /** List of artifacts types supported by the orchestrator. */
    private String[] types;
}
