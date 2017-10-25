package alien4cloud.plugin.archives;

import java.util.List;

import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.plugin.exception.PluginArchiveException;

/**
 * Plugin component that allows to provide archives to be indexed into alien4cloud catalog.
 */
public interface IArchiveProviderPlugin {
    /**
     * Get archives provided by the plugin. The components defined will be indexed into the catalog.
     *
     * Note that theses archives should not contains any topologies as they will be ignored by alien.
     *
     * @return The archives provided by the plugin.
     */
    List<PluginArchive> getArchives() throws PluginArchiveException;
}