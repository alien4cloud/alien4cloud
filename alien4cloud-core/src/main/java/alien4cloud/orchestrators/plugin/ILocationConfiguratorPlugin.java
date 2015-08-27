package alien4cloud.orchestrators.plugin;

import java.util.List;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.model.PluginArchive;

/**
 * Plugin component that allows the automatic configuration of a location.
 */
public interface ILocationConfiguratorPlugin {
    /**
     * Get archives provided by the plugin. They contains all the types that are used to configure the plugin or that the plugin can eventually support.
     * Note that theses archives should not contains any topologies as they will be ignored by alien.
     *
     * @return The archives provided by the plugin.
     */
    List<PluginArchive> pluginArchives();

    /**
     * Get a list of the location resources types. If a type is abstract it won't be used for matching but only as a helper for plugin auto-configuration.
     * For example Image and Flavor should be abstract while compute should be implemented.
     *
     * @return A list of location resources types.
     */
    List<String> getResourcesTypes();

    /**
     * Auto-configure the instances of location resources.
     *
     * @return A list of locations resources templates that users can define or null if the plugin doesn't support auto-configuration of resources..
     */
    List<LocationResourceTemplate> instances();
}