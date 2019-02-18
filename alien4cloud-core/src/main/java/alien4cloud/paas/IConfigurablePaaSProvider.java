package alien4cloud.paas;

import alien4cloud.paas.exception.PluginConfigurationException;

public interface IConfigurablePaaSProvider<T> extends IPaaSProvider {

    /**
     * Set / apply a configuration for a PaaS provider
     *
     * @param orchestratorId The id of the orchestrator.
     * @param configuration The configuration object as edited by the user.
     * @throws alien4cloud.paas.exception.PluginConfigurationException In case the PaaS provider configuration is incorrect.
     */
    @Deprecated
    void setConfiguration(String orchestratorId, T configuration) throws PluginConfigurationException;
}
