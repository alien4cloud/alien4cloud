package alien4cloud.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Map a usage of a resource by another one.
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class Usage {
    /** Name of the resource that uses. */
    private String resourceName;
    /** Type of the resource that uses. */
    private String resourceType;
    /** Id of the resource that uses */
    private String resourceId;
    /** Workspace of the resource that uses the CSAR, can be null if this notion does not exist on the resource **/
    private String workspace;
}