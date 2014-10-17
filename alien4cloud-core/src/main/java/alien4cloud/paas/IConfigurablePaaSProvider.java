package alien4cloud.paas;

import alien4cloud.paas.exception.PluginConfigurationException;

/**
 * Interface to configure a PaaS provider.
 * 
 * @param <T> The type of the configuration object for the PaaS provider.
 */
public interface IConfigurablePaaSProvider<T> {
    /**
     * Get the default configuration object of the PaaS provider
     * 
     * @return A configuration object of type T
     */
    T getDefaultConfiguration();

    /**
     * Set / apply a configuration for a PaaS provider
     * 
     * @param configuration The configuration object as edited by the user.
     * @throws PluginConfigurationException In case the PaaS provider configuration is incorrect.
     */
    void setConfiguration(T configuration) throws PluginConfigurationException;
}