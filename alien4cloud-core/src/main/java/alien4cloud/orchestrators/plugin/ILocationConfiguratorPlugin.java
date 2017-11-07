package alien4cloud.orchestrators.plugin;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
    default List<PluginArchive> pluginArchives() {
        return Lists.newArrayList();
    };

    /**
     * Get a list of the location resources types. If a type is abstract it won't be used for matching but only as a helper for plugin auto-configuration.
     * For example Image and Flavor should be abstract while compute should be implemented.
     *
     * @return A list of location resources types.
     */
    default List<String> getResourcesTypes() {
        return Lists.newArrayList();
    };

    /**
     * Return the list of matching configurations used to match the nodes provided by the location.
     * 
     * @return A list of MatchingConfigurations for the types provided by the location.
     */
    default Map<String, MatchingConfiguration> getMatchingConfigurations() {
        return Maps.newHashMap();
    };

    /**
     * Auto-configure the instances of location resources.
     *
     * @param resourceAccessor Instances that allows to query resources currently configured for the location instances. Auto-configuration of some elements may
     *            indeed require access to some manually configured resources.
     * @return A list of locations resources templates that users can define or null if the plugin doesn't support auto-configuration of resources..
     */
    default List<LocationResourceTemplate> instances(ILocationResourceAccessor resourceAccessor) throws UnsupportedOperationException {
        return null;
    };

    /**
     * Get a list of the policies types supported by the location.
     *
     * @return A list of location policies types.
     */
    default List<String> getPoliciesTypes() {
        return Lists.newArrayList();
    }
}