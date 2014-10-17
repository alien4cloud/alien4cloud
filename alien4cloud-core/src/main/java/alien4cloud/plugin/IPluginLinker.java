package alien4cloud.plugin;

import java.util.List;

/**
 * A plugin linker is able to link some classes out of a plugin application context into alien's internal mechanisms.
 *
 * @author luc boutier
 *
 * @param <T> The type of implementation that the plugin linker supports.
 */
public interface IPluginLinker<T> {
    /**
     * Link an instance of a linkable class from a plugin
     *
     * @param pluginId The id of the plugin.
     * @param instanceId The id of the instance to actually link.
     * @param instance The instance to link.
     */
    void link(String pluginId, String instanceId, T instance);

    /**
     * Unlink all instance of a given plugin.
     *
     * @param pluginId The id of the plugin to unlink.
     */
    void unlink(String pluginId);

    /**
     * Get usage for a given plugin.
     *
     * @param pluginId The id of the used plugin.
     * @return a list of all plugin usages.
     */
    List<PluginUsage> usage(String pluginId);
}
