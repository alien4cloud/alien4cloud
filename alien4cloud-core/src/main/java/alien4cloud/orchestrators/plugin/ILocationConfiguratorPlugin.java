package alien4cloud.orchestrators.plugin;

import alien4cloud.model.orchestrators.locations.LocationResourceDefinition;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.tosca.model.ArchiveRoot;

import java.util.List;

/**
 * Plugin component that allows the automatic configuration of a location.
 */
public interface ILocationConfiguratorPlugin {
    /**
     * Get archives provided by the plugin. They contains all the types that are used to configure the plugin or that the plugin can eventually support.
     * Theses archives are placed in a specific tosca index and by default are specific to the plugin.
     *
     * @return The archives provided by the plugin.
     */
    List<ArchiveRoot> pluginArchives();

    /**
     * Get a list of the location resources types.
     *
     * @return A list of location resources types.
     */
    List<LocationResourceDefinition> definitions();

    /**
     * Auto-configure the instances of location resources.
     *
     * @return A list of locations resources templates that users can define or null if the plugin doesn't support auto-configuration of resources..
     */
    List<LocationResourceTemplate> instances();
}