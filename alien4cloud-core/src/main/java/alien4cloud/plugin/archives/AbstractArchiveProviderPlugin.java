package alien4cloud.plugin.archives;

import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;

import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.plugin.exception.PluginArchiveException;

/**
 *
 * Abstract implementation of {@link IArchiveProviderPlugin}. <br/>
 *
 * Implementation should be done inside plugins, and not inside Alien4Cloud main projects .<br/>
 * Every willing plugin can annotate the implementation with @{@link org.springframework.stereotype.Component}. Thus the related
 * {@link AbstractPluginArchiveService} bean will be injected
 */
public abstract class AbstractArchiveProviderPlugin implements IArchiveProviderPlugin {

    @Inject
    private AbstractPluginArchiveService archiveService;

    private List<PluginArchive> archives;

    private void parsePluginArchives(String[] paths) throws PluginArchiveException {
        this.archives = Lists.newArrayList();
        for (String path : paths) {
            this.archives.add(archiveService.parse(path));
        }
    }

    @Override
    public List<PluginArchive> getArchives() throws PluginArchiveException {
        if (this.archives == null) {
            parsePluginArchives(getArchivesPaths());
        }
        return this.archives;
    }

    /**
     * Get the paths in the plugin where the archives to parse are located.
     * 
     * @return list of String each representing a path of an archive to parse
     */
    protected abstract String[] getArchivesPaths();
}
