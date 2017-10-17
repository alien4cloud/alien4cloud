package alien4cloud.orchestrators.plugin;

import java.util.List;
import java.util.Map;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
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
     * Return the list of matching configurations used to match the nodes provided by the location.
     * 
     * @return A list of MatchingConfigurations for the types provided by the location.
     */
    Map<String, MatchingConfiguration> getMatchingConfigurations();

    /**
     * Auto-configure the instances of location resources.
     *
     * @param resourceAccessor Instances that allows to query resources currently configured for the location instances. Auto-configuration of some elements may
     *            indeed require access to some manually configured resources.
     * @return A list of locations resources templates that users can define or null if the plugin doesn't support auto-configuration of resources..
     */
    List<LocationResourceTemplate> instances(ILocationResourceAccessor resourceAccessor) throws UnsupportedOperationException;

    /**
     * Get a list of the policies types supported by the location.
     *
     * @return A list of location policies types.
     */
    default List<String> getPoliciesTypes() {
        return null;
    }
}