package alien4cloud.plugin.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Plugin descriptor (representation of the YAML that describe a plugin).
 */
@Getter
@Setter
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
    /** Entry point for the ui plugin (js file) to be loaded using require js. */
    private String uiEntryPoint;
    /** Plugin dependencies (other plugins required for this plugin to run). **/
    private String[] dependencies;
    /** Descriptor within the plugin. */
    private PluginComponentDescriptor[] componentDescriptors;
}