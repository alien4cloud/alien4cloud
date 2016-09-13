package alien4cloud.rest.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result for a component version.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComponentVersionsResult {
    /** The id of the element (including hash). */
    private String id;
    /** The version of the element. */
    private String version;
}