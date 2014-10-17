package alien4cloud.plugin;

import lombok.Getter;
import lombok.Setter;

/**
 * Plugin descriptor (representation of the YAML that describe a plugin).
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class PluginDescriptor {
    /** Unique id of the plugin. */
    private String id;
    /** Name of the plugin. */
    private String name;
    /** Version of the plugin. */
    private String version;
    /** Description text for the plugin. */
    private String description;
    /** The name of the configuration class to load the plugin's spring context. */
    private String configurationClass;
    /** Descriptor within the plugin. */
    private PluginComponentDescriptor[] componentDescriptors;
}