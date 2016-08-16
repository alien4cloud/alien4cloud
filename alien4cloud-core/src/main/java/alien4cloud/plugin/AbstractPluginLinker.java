package alien4cloud.plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

import alien4cloud.plugin.exception.MissingPluginException;

/**
 * Utility abstract implementation of an {@link IPluginLinker}.
 * 
 * @param <T> The type of element that the plugin linker supports.
 */
public abstract class AbstractPluginLinker<T> implements IPluginLinker<T> {
    private ConcurrentMap<String, Map<String, T>> instancesByPlugins = Maps.newConcurrentMap();

    @Override
    public void link(String pluginId, String instanceId, T instance) {
        Map<String, T> pluginInstances = instancesByPlugins.get(pluginId);
        if (pluginInstances == null) {
            Map<String, T> map = Maps.newHashMap();
            instancesByPlugins.putIfAbsent(pluginId, map);
            pluginInstances = instancesByPlugins.get(pluginId);
        }
        pluginInstances.put(instanceId, instance);
    }

    @Override
    public void unlink(String pluginId) {
        instancesByPlugins.remove(pluginId);
    }

    /**
     * Get the bean of a plugin based on it's id and plugin bean name
     * 
     * @param pluginId The id of the plugin that should contains the bean.
     * @param pluginBeanName The name of the bean to actually get.
     * @return The bean.
     */
    public T getPluginBean(String pluginId, String pluginBeanName) {
        Map<String, T> pluginBeans = instancesByPlugins.get(pluginId);
        if (pluginBeans == null) {
            throw new MissingPluginException("The plugin <" + pluginId + "> cannot be found", false);
        }
        T pluginBean = pluginBeans.get(pluginBeanName);
        if (pluginBean == null) {
            throw new MissingPluginException("The bean <" + pluginBeanName + "> from plugin <" + pluginId + "> cannot be found", true);
        }
        return pluginBean;
    }

    /**
     * Get the first bean found of a plugin based on it's id
     *
     * @param pluginId The id of the plugin that should contains the bean.
     * @return The bean.
     */
    public T getSinglePluginBean(String pluginId) {
        Map<String, T> pluginBeans = instancesByPlugins.get(pluginId);
        if (pluginBeans == null || pluginBeans.isEmpty()) {
            throw new MissingPluginException("The plugin <" + pluginId + "> cannot be found", false);
        }
        return pluginBeans.values().iterator().next();
    }

    /**
     * Get all the instances mapped by pluginId -> pluginBeanName -> instance.
     * 
     * @return The map of instances by pluginId and pluginBeanName
     */
    public Map<String, Map<String, T>> getInstancesByPlugins() {
        return instancesByPlugins;
    }
}