package alien4cloud.plugin.mock;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.common.AlienConstants;
import alien4cloud.deployment.matching.services.nodes.MatchingConfigurations;
import alien4cloud.deployment.matching.services.nodes.MatchingConfigurationsParser;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceGeneratorService;
import alien4cloud.orchestrators.locations.services.LocationResourceGeneratorService.ComputeContext;
import alien4cloud.orchestrators.locations.services.LocationResourceGeneratorService.ImageFlavorContext;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.paas.exception.PluginParseException;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Configure resources for the openstack location type.
 */
@Slf4j
@Component
@Scope("prototype")
public class MockOpenStackLocationConfigurer implements ILocationConfiguratorPlugin {
    @Inject
    private ArchiveParser archiveParser;
    @Inject
    private MatchingConfigurationsParser matchingConfigurationsParser;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private ManagedPlugin selfContext;
    @Inject
    private LocationResourceGeneratorService resourceGeneratorService;

    private List<PluginArchive> archives;

    private static final String IMAGE_ID_PROP = "imageId";
    private static final String FLAVOR_ID_PROP = "flavorId";

    @Override
    public List<PluginArchive> pluginArchives() throws PluginParseException {
        if (archives == null) {
            try {
                archives = parseArchives();
            } catch (ParsingException e) {
                log.error(e.getMessage());
                throw new PluginParseException(e.getMessage());
            }
        }
        return archives;
    }

    private List<PluginArchive> parseArchives() throws ParsingException {
        List<PluginArchive> archives = Lists.newArrayList();
        addToAchive(archives, "openstack/mock-openstack-resources");
        addToAchive(archives, "openstack/mock-resources");
        return archives;
    }

    private void addToAchive(List<PluginArchive> archives, String path) throws ParsingException {
        Path archivePath = selfContext.getPluginPath().resolve(path);
        // Parse the archives
        ParsingResult<ArchiveRoot> result = archiveParser.parseDir(archivePath, AlienConstants.GLOBAL_WORKSPACE_ID);
        PluginArchive pluginArchive = new PluginArchive(result.getResult(), archivePath);
        archives.add(pluginArchive);
    }

    @Override
    public List<String> getResourcesTypes() {
        return Lists.newArrayList("alien.nodes.mock.openstack.Image", "alien.nodes.mock.openstack.Flavor", "alien.nodes.mock.Compute",
                "alien.nodes.mock.BlockStorage", "alien.nodes.mock.Network");
    }

    @Override
    public Map<String, MatchingConfiguration> getMatchingConfigurations() {
        Path matchingConfigPath = selfContext.getPluginPath().resolve("openstack/mock-resources-matching-config.yml");
        MatchingConfigurations matchingConfigurations = null;
        try {
            matchingConfigurations = matchingConfigurationsParser.parseFile(matchingConfigPath).getResult();
        } catch (ParsingException e) {
            return Maps.newHashMap();
        }
        return matchingConfigurations.getMatchingConfigurations();
    }

    @Override
    public List<LocationResourceTemplate> instances(ILocationResourceAccessor resourceAccessor) {
        ImageFlavorContext imageContext = resourceGeneratorService.buildContext("alien.nodes.mock.openstack.Image", "id", resourceAccessor);
        ImageFlavorContext flavorContext = resourceGeneratorService.buildContext("alien.nodes.mock.openstack.Flavor", "id", resourceAccessor);
        boolean canProceed = true;
        if (CollectionUtils.isEmpty(imageContext.getTemplates())) {
            log.warn("At least one configured image resource is required for the auto-configuration");
            canProceed = false;
        }
        if (CollectionUtils.isEmpty(flavorContext.getTemplates())) {
            log.warn("At least one configured flavor resource is required for the auto-configuration");
            canProceed = false;
        }
        if (!canProceed) {
            log.warn("Skipping auto configuration");
            return null;
        }
        ComputeContext computeContext = resourceGeneratorService.buildComputeContext("alien.nodes.mock.Compute", null, IMAGE_ID_PROP, FLAVOR_ID_PROP,
                resourceAccessor);

        return resourceGeneratorService.generateComputeFromImageAndFlavor(imageContext, flavorContext, computeContext, resourceAccessor);
    }
}
