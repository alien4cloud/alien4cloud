package alien4cloud.orchestrators.locations.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

import alien4cloud.component.portability.PortabilityPropertyEnum;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.common.Tag;
import alien4cloud.model.common.Usage;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.tosca.ArchiveIndexer;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Manage the indexing of TOSCA archives.
 */
@Slf4j
@Component
@SuppressWarnings("rawtypes")
public class PluginArchiveIndexer {
    @Inject
    private OrchestratorService orchestratorService;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private CsarService csarService;
    @Inject
    private ArchiveIndexer archiveIndexer;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Ensure that location archives are indexed, note that by default the archives visibility is not public.
     *
     * @param orchestrator the orchestrator for which to index archives.
     * @param location The location of the orchestrator for which to index archives.
     */
    public Set<CSARDependency> indexLocationArchives(Orchestrator orchestrator, Location location) {
        IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) orchestratorPluginService.getOrFail(orchestrator.getId());
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());

        // ensure that the plugin archives for this location are imported in the
        List<PluginArchive> pluginArchives = configuratorPlugin.pluginArchives();
        Set<CSARDependency> dependencies = Sets.newHashSet();

        if (pluginArchives == null) {
            return dependencies;
        }

        // index archive here if not already indexed
        for (PluginArchive pluginArchive : pluginArchives) {
            ArchiveRoot archive = pluginArchive.getArchive();
            Csar csar = csarService.getIfExists(archive.getArchive().getName(), archive.getArchive().getVersion());

            // TODO Do we really want to check this? as we might not be able to override location snapshot archives!
            if (csar == null) {
                // index the required archive
                indexArchive(pluginArchive, orchestrator, location);
            } else {
                // TODO Link csar and archive elements to the location
                log.debug("Archive {}:{} from plugin {}:{} location {} already exists in the repository and won't be updated.", archive.getArchive().getName(),
                        archive.getArchive().getVersion(), orchestrator.getPluginId(), orchestrator.getPluginBean(), location.getInfrastructureType());
            }
            if (archive.getArchive().getDependencies() != null) {
                dependencies.addAll(archive.getArchive().getDependencies());
            }
            dependencies.add(new CSARDependency(archive.getArchive().getName(), archive.getArchive().getVersion()));
        }

        return dependencies;
    }

    public void indexOrchestratorArchives(IOrchestratorPluginFactory<IOrchestratorPlugin<?>, ?> orchestratorFactory) {
        try {
            IOrchestratorPlugin<?> orchestratorInstance = orchestratorFactory.newInstance();
            for (PluginArchive pluginArchive : orchestratorInstance.pluginArchives()) {
                injectPortabilityInfos(pluginArchive.getArchive().getNodeTypes().values(), orchestratorFactory, null);
                List<ParsingError> parsingErrors = Lists.newArrayList();
                archiveIndexer.importArchive(pluginArchive.getArchive(), pluginArchive.getArchiveFilePath(), parsingErrors);
            }
        } catch (CSARVersionAlreadyExistsException e) {
            log.info("Skipping orchestrator archive import as the released version already exists in the repository.");
        }
    }

    private void indexArchive(PluginArchive pluginArchive, Orchestrator orchestrator, Location location) {
        ArchiveRoot archive = pluginArchive.getArchive();

        // inject a specific tag to allow components catalog filtering search
        injectWorkSpace(archive.getNodeTypes().values(), orchestrator, location);
        injectWorkSpace(archive.getArtifactTypes().values(), orchestrator, location);
        injectWorkSpace(archive.getCapabilityTypes().values(), orchestrator, location);
        injectWorkSpace(archive.getRelationshipTypes().values(), orchestrator, location);

        // inject portability informations
        injectPortabilityInfos(archive.getNodeTypes().values(), orchestrator, location);

        List<ParsingError> parsingErrors = Lists.newArrayList();

        // index the archive in alien catalog
        try {
            archiveIndexer.importArchive(archive, pluginArchive.getArchiveFilePath(), parsingErrors);
        } catch (CSARVersionAlreadyExistsException e) {
            log.info("Skipping location archive import as the released version already exists in the repository.");
        }
    }

    private void injectPortabilityInfos(Collection<IndexedNodeType> collection, Orchestrator orchestrator, Location location) {
        IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestrator);
        injectPortabilityInfos(collection, orchestratorFactory, location);
    }

    private void injectPortabilityInfos(Collection<IndexedNodeType> collection, IOrchestratorPluginFactory orchestratorFactory, Location location) {
        if (CollectionUtils.isNotEmpty(collection)) {
            for (IndexedNodeType nodeType : collection) {
                addOchestratorAndLocationInfo(nodeType, orchestratorFactory, location);
            }
        }
    }

    private void addOchestratorAndLocationInfo(IndexedNodeType nodeType, IOrchestratorPluginFactory orchestratorFactory, Location location) {
        if (nodeType.getPortability() == null) {
            nodeType.setPortability(Maps.<String, AbstractPropertyValue> newHashMap());
        }
        addInPortabilityProperty(nodeType.getPortability(), PortabilityPropertyEnum.ORCHESTRATORS_KEY, orchestratorFactory.getType());

        if (location != null) {
            addInPortabilityProperty(nodeType.getPortability(), PortabilityPropertyEnum.IAASS_KEY, location.getInfrastructureType());
        }
    }

    private void addInPortabilityProperty(Map<String, AbstractPropertyValue> portabilityMap, String portabilityKey, Object infoToAdd) {
        ListPropertyValue propertyValue = portabilityMap.get(portabilityKey) != null ? (ListPropertyValue) portabilityMap.get(portabilityKey)
                : new ListPropertyValue(Lists.newArrayList());
        propertyValue.getValue().add(infoToAdd);
        alien4cloud.utils.CollectionUtils.ensureUnitictyOfValues(propertyValue.getValue());
        portabilityMap.put(portabilityKey, propertyValue);
    }

    private void injectWorkSpace(Collection<? extends IndexedToscaElement> elements, Orchestrator orchestrator, Location location) {
        for (IndexedToscaElement element : elements) {
            if (element.getTags() == null) {
                element.setTags(new ArrayList<Tag>());
            }
            element.getTags().add(new Tag("alien-workspace-id", orchestrator.getId() + ":" + location.getId()));
            element.getTags().add(new Tag("alien-workspace-name", orchestrator.getName() + " - " + location.getName()));
        }
    }

    /**
     * Delete all archives related to a location, if not exposed or used by another location
     *
     * @param location
     * @return Map of usages per archives if found (that means the deletion wasn't performed successfully), null if everything went well.
     */
    public Map<Csar, List<Usage>> deleteArchives(Location location) {
        ILocationConfiguratorPlugin configuratorPlugin = getConfiguratorPlugin(location);
        List<PluginArchive> pluginArchives = configuratorPlugin.pluginArchives();
        // abort if no archive is exposed by this location
        if (CollectionUtils.isEmpty(pluginArchives)) {
            return null;
        }
        Set<String> allExposedArchivesIds = getAllExposedArchivesIdsExluding(location);
        Map<Csar, List<Usage>> usages = Maps.newHashMap();
        for (PluginArchive pluginArchive : pluginArchives) {
            Csar csar = pluginArchive.getArchive().getArchive();
            // only delete if no other location exposed this archive
            if (!allExposedArchivesIds.contains(csar.getId())) {
                List<Usage> csarUsage = csarService.deleteCsarWithElements(csar);
                if (CollectionUtils.isNotEmpty(csarUsage)) {
                    usages.put(csar, csarUsage);
                }
            }
        }
        return usages.isEmpty() ? null : usages;
    }

    private ILocationConfiguratorPlugin getConfiguratorPlugin(Location location) {
        ILocationConfiguratorPlugin configuratorPlugin;
        try {
            IOrchestratorPlugin<Object> orchestratorInstance = orchestratorPluginService.getOrFail(location.getOrchestratorId());
            configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());
        } catch (OrchestratorDisabledException e) {
            IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestratorService.getOrFail(location.getOrchestratorId()));
            IOrchestratorPlugin<Object> orchestratorInstance = orchestratorFactory.newInstance();
            configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());
            orchestratorFactory.destroy(orchestratorInstance);
        }
        return configuratorPlugin;
    }

    private Set<String> getAllExposedArchivesIdsExluding(Location excludedLocation) {
        // exclude a location from the search
        QueryBuilder query = QueryBuilders.boolQuery()
                .mustNot(QueryBuilders.idsQuery(Location.class.getSimpleName().toLowerCase()).ids(excludedLocation.getId()));
        List<Location> locations = alienDAO.customFindAll(Location.class, query);
        Set<String> archiveIds = Sets.newHashSet();
        if (locations != null) {
            for (Location location : locations) {
                ILocationConfiguratorPlugin configuratorPlugin = getConfiguratorPlugin(location);
                List<PluginArchive> pluginArchives = configuratorPlugin.pluginArchives();
                for (PluginArchive pluginArchive : pluginArchives) {
                    archiveIds.add(pluginArchive.getArchive().getArchive().getId());
                }
            }
        }

        return archiveIds;
    }
}