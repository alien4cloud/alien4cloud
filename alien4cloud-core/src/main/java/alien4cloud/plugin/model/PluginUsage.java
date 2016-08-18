package alien4cloud.plugin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Map a usage of a plugin by a resource.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class PluginUsage {
    /** Id of the resource that uses the plugin. */
    private String resourceId;
    /** Name of the resource used by the plugin. */
    private String resourceName;
    /** Type of the resource that uses the plugin (Application for example). */
    private String resourceType;
}