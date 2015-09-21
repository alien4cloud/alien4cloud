package alien4cloud.plugin.mock;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.topology.TopologyService;
import alien4cloud.tosca.ArchiveParser;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Configure resources for the openstack location type.
 */
@Slf4j
@Component
public class MockOpenStackLocationConfigurer implements ILocationConfiguratorPlugin {
    @Inject
    private ArchiveParser archiveParser;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private ManagedPlugin selfContext;
    @Inject
    private TopologyService topoService;

    private List<PluginArchive> archives;

    private static final String IMAGE_ID_PROP = "imageId";
    private static final String FLAVOR_ID_PROP = "flavorId";

    @PostConstruct
    private void postConstruct() {
        archives = parseArchives();
    }

    @Override
    public List<PluginArchive> pluginArchives() {
        return archives;
    }

    private List<PluginArchive> parseArchives() {
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
        return Lists.newArrayList("alien.nodes.mock.openstack.Image", "alien.nodes.mock.openstack.Flavor", "alien.nodes.mock.Compute");
    }

    @Override
    public List<LocationResourceTemplate> instances(ILocationResourceAccessor resourceAccessor) {
        List<LocationResourceTemplate> configuredImages = resourceAccessor.getResources("alien.nodes.mock.openstack.Image");
        List<LocationResourceTemplate> configuredFlavors = resourceAccessor.getResources("alien.nodes.mock.openstack.Flavor");
        boolean canProceed = true;
        if (CollectionUtils.isEmpty(configuredImages)) {
            log.warn("At least one configured image resource is required for the auto-configuration");
            canProceed = false;
        }
        if (CollectionUtils.isEmpty(configuredFlavors)) {
            log.warn("At least one configured flavor resource is required for the auto-configuration");
            canProceed = false;
        }

        if (!canProceed) {
            log.warn("Skipping auto configuration");
            return null;
        }

        List<LocationResourceTemplate> generatedComputes = Lists.newArrayList();
        Map<IndexedNodeType, Csar> computeTypes = getSupportedCompute();

        // TODO auto-generate computes from the images and flavors
        // create a few images and flavors and then mix-them up to generate compute templates
        int count = 0;
        for (LocationResourceTemplate image : configuredImages) {
            for (LocationResourceTemplate flavor : configuredFlavors) {
                for (Entry<IndexedNodeType, Csar> typeEntry : computeTypes.entrySet()) {
                    IndexedNodeType indexedNodeType = typeEntry.getKey();
                    Csar csar = typeEntry.getValue();
                    NodeTemplate node = topoService.buildNodeTemplate(csar.getDependencies(), indexedNodeType, null);
                    // set the imageId
                    node.getProperties().put(IMAGE_ID_PROP, image.getTemplate().getProperties().get("id"));
                    // set the flavorId
                    node.getProperties().put(FLAVOR_ID_PROP, flavor.getTemplate().getProperties().get("id"));

                    LocationResourceTemplate resource = new LocationResourceTemplate();
                    resource.setService(false);
                    resource.setTemplate(node);
                    resource.setName("Compute" + count);
                    count++;

                    generatedComputes.add(resource);
                }
            }
        }

        return generatedComputes;
    }

    private Map<IndexedNodeType, Csar> getSupportedCompute() {
        Map<IndexedNodeType, Csar> toReturn = Maps.newHashMap();
        for (PluginArchive archive : archives) {
            for (IndexedNodeType nodeType : archive.getArchive().getNodeTypes().values()) {
                if (ToscaUtils.isFromType("tosca.nodes.Compute", nodeType)) {
                    toReturn.put(nodeType, archive.getArchive().getArchive());
                }
            }
        }
        return toReturn;
    }
}
