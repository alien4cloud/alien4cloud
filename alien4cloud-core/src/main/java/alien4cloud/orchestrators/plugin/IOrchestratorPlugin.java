package alien4cloud.orchestrators.plugin;

import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.IPaaSProvider;

/**
 * Interface used to communicate with an orchestrator.
 */
public interface IOrchestratorPlugin<T> extends IConfigurablePaaSProvider<T>, IPaaSProvider {
    /**
     * Return a configurator instance for a given location type.
     * 
     * @param locationType The type of location for which to get a location configurator.
     * @return The configurator plugin.
     */
    ILocationConfiguratorPlugin getConfigurator(String locationType);
}