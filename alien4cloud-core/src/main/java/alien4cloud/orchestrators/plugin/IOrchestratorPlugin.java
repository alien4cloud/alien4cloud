package alien4cloud.orchestrators.plugin;

import java.util.List;

import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.paas.IConfigurablePaaSProvider;

/**
 * Interface used to communicate with an orchestrator.
 */
public interface IOrchestratorPlugin<T> extends IConfigurablePaaSProvider<T> {

    /**
     * Get archives provided by the plugin. They contains all the types that are used to configure the plugin or that the plugin can eventually support.
     * Note that theses archives should not contains any topologies as they will be ignored by alien.
     *
     * @return The archives provided by the plugin.
     */
    List<PluginArchive> pluginArchives();
}