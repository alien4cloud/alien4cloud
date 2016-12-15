package alien4cloud.topology;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a dependency conflict for a source CSAR in a topology.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DependencyConflictDTO {

    private String source;
    private String expected;
    private String actualVersion;
}
