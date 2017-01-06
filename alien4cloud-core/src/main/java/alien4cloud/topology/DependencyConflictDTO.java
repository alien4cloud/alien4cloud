package alien4cloud.topology;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for dependency conflicts : source depends on dependency, which resolved to resolvedVersion.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DependencyConflictDTO {

    /** <strong>Source</strong> is the topology's dependency <code>archiveName</code>
     * that depends, directly or transitively, on <strong>dependency</strong>. */
    private String source;
    /** <strong>Dependency</strong> is the <code>archiveName:archiveVersion</code> that causes a dependency conflict. */
    private String dependency;
    /** Version of the dependency as actually resolved in the topology. */
    private String resolvedVersion;
}
