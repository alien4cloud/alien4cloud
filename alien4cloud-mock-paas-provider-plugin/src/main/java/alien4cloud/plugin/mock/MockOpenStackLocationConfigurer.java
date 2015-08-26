package alien4cloud.plugin.mock;

import java.nio.file.Path;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import alien4cloud.model.orchestrators.locations.LocationResourceDefinition;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.tosca.ArchiveParser;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

import com.google.common.collect.Lists;

/**
 * Configure resources for the openstack location type.
 */
@Slf4j
@AllArgsConstructor
public class MockOpenStackLocationConfigurer implements ILocationConfiguratorPlugin {
    private ArchiveParser archiveParser;
    private PluginManager pluginManager;
    private ManagedPlugin selfContext;

    @Override
    public List<PluginArchive> pluginArchives() {
        List<PluginArchive> archives = Lists.newArrayList();

        Path archivePath = selfContext.getPluginPath().resolve("openstack/openstack-resources.yaml");
        // Parse the archives
        try {
            ParsingResult<ArchiveRoot> result = archiveParser.parse(archivePath);
            PluginArchive pluginArchive = new PluginArchive(result.getResult(), archivePath);
            archives.add(pluginArchive);
        } catch (ParsingException e) {
            log.error("Failed to parse archive, plugin won't work as expected", e);
        }

        return archives;
    }

    @Override
    public List<LocationResourceDefinition> definitions() {
        return null;
    }

    @Override
    public List<LocationResourceTemplate> instances() {
        return null;
    }
}
