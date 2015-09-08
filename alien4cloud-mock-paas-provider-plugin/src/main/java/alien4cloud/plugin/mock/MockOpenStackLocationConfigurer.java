package alien4cloud.plugin.mock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import alien4cloud.model.topology.NodeTemplate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
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
        addToAchive(archives, "openstack/openstack-resources.yaml.zip");
        addToAchive(archives, "openstack/mock-resources.yaml.zip");
        return archives;
    }

    private void addToAchive(List<PluginArchive> archives, String path) {
        Path archivePath = selfContext.getPluginPath().resolve(path);
        // Parse the archives
        try {
            ParsingResult<ArchiveRoot> result = archiveParser.parse(archivePath);
            PluginArchive pluginArchive = new PluginArchive(result.getResult(), archivePath);
            archives.add(pluginArchive);
        } catch (ParsingException e) {
            log.error("Failed to parse archive, plugin won't work as expected", e);
        }
    }

    @Override
    public List<String> getResourcesTypes() {
        return Lists.newArrayList("alien.nodes.mock.openstack.Image", "alien.nodes.mock.openstack.Flavor",
                "alien.nodes.mock.Compute");
    }

    @Override
    public List<LocationResourceTemplate> instances(ILocationResourceAccessor resourceAccessor) {
        // create a few images and flavors and then mix-them up to generate compute templates
        LocationResourceTemplate template = new LocationResourceTemplate();

        NodeTemplate nodeTemplate = new NodeTemplate();

        template.setService(false);
        template.setTemplate(nodeTemplate);

        return null;
    }
}
