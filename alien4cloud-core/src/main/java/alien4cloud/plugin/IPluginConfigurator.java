package alien4cloud.plugin;

import alien4cloud.plugin.exception.PluginConfigurationException;

/**
 * Interface for plugin configuration objects
 * 
 * @author 'Igor Ngouagna'
 */
public interface IPluginConfigurator<T> {

    /**
     * Get the default configuration object of the plugin
     * 
     * @return A configuration object of type T
     */
    T getDefaultConfiguration();

    /**
     * Set / apply a configuration for a plugin
     * 
     * @param configuration The configuration object as edited by the user.
     * @throws PluginConfigurationException In case the plugin configuration is incorrect.
     */
    void setConfiguration(T configuration) throws PluginConfigurationException;
}