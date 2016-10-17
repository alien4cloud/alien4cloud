package alien4cloud.orchestrators.locations.services;

import java.util.*;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.ArchiveIndexer;
import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeIndexerService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.events.LocationArchiveDeleteRequested;
import alien4cloud.events.LocationTypeIndexed;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.common.Tag;
import alien4cloud.model.common.Usage;
import alien4cloud.model.components.CSARSource;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import lombok.extern.slf4j.Slf4j;

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
    private ICsarService csarService;
    @Inject
    private ArchiveIndexer archiveIndexer;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ApplicationContext applicationContext;
    @Inject
    private IToscaTypeIndexerService csarRepositoryIndexerService;

    /**
     * Ensure that location archives are indexed.
     *
     * @param orchestrator the orchestrator for which to index archives.
     * @param location The location of the orchestrator for which to index archives.
     */
    public Set<CSARDependency> indexLocationArchives(Orchestrator orchestrator, Location location) {
        IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) orchestratorPluginService.getOrFail(orchestrator.getId());
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());

        // ensure that the plugin archives for this location are imported in the catalog
        List<PluginArchive> pluginArchives = configuratorPlugin.pluginArchives();
        Set<CSARDependency> dependencies = Sets.newHashSet();

        if (pluginArchives == null) {
            return dependencies;
        }

        // index archive here if not already indexed
        for (PluginArchive pluginArchive : pluginArchives) {
            ArchiveRoot archive = pluginArchive.getArchive();
            Csar csar = csarService.get(archive.getArchive().getName(), archive.getArchive().getVersion());
            String lastParsedHash = null;

            if (csar == null) {
                // index the required archive
                indexArchive(pluginArchive, orchestrator, location);
                lastParsedHash = archive.getArchive().getHash();
            } else {
                lastParsedHash = csar.getHash();
            }
            if (archive.getArchive().getDependencies() != null) {
                dependencies.addAll(archive.getArchive().getDependencies());
            }
            dependencies.add(new CSARDependency(archive.getArchive().getName(), archive.getArchive().getVersion(), lastParsedHash));
        }

        return dependencies;
    }

    /**
     * Index archives defined at the orchestrator level by a plugin.
     *
     * @param orchestratorFactory The orchestrator factory.
     * @param orchestratorInstance The instance of the orchestrator (created by the factory).
     */
    public void indexOrchestratorArchives(IOrchestratorPluginFactory<IOrchestratorPlugin<?>, ?> orchestratorFactory,
            IOrchestratorPlugin<Object> orchestratorInstance) {
        for (PluginArchive pluginArchive : orchestratorInstance.pluginArchives()) {
            try {
                archiveIndexer.importArchive(pluginArchive.getArchive(), CSARSource.ORCHESTRATOR, pluginArchive.getArchiveFilePath(),
                        Lists.<ParsingError> newArrayList());
                publishLocationTypeIndexedEvent(pluginArchive.getArchive().getNodeTypes().values(), orchestratorFactory, null);
            } catch (AlreadyExistException e) {
                log.info("Skipping orchestrator archive import as the released version already exists in the repository. " + e.getMessage());
            } catch (CSARUsedInActiveDeployment e) {
                log.info("Skipping orchestrator archive import as it is used in an active deployment. " + e.getMessage());
            }
        }
    }

    private void indexArchive(PluginArchive pluginArchive, Orchestrator orchestrator, Location location) {
        ArchiveRoot archive = pluginArchive.getArchive();

        // inject a specific tag to allow components catalog filtering search
        injectWorkSpace(archive.getNodeTypes().values(), orchestrator, location);
        injectWorkSpace(archive.getArtifactTypes().values(), orchestrator, location);
        injectWorkSpace(archive.getCapabilityTypes().values(), orchestrator, location);
        injectWorkSpace(archive.getRelationshipTypes().values(), orchestrator, location);

        List<ParsingError> parsingErrors = Lists.newArrayList();
        // index the archive in alien catalog
        try {
            archiveIndexer.importArchive(archive, CSARSource.ORCHESTRATOR, pluginArchive.getArchiveFilePath(), parsingErrors);
        } catch (AlreadyExistException e) {
            log.info("Skipping location archive import as the released version already exists in the repository.");
        } catch (CSARUsedInActiveDeployment e) {
            log.info("Skipping orchestrator archive import as it is used in an active deployment. " + e.getMessage());
        }

        // Publish event to allow plugins to post-process elements (portability plugin for example).
        publishLocationTypeIndexedEvent(archive.getNodeTypes().values(), orchestrator, location);
    }

    private void publishLocationTypeIndexedEvent(Collection<NodeType> collection, Orchestrator orchestrator, Location location) {
        IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestrator);
        publishLocationTypeIndexedEvent(collection, orchestratorFactory, location);
    }

    private void publishLocationTypeIndexedEvent(Collection<NodeType> collection, IOrchestratorPluginFactory orchestratorFactory, Location location) {
        if (CollectionUtils.isNotEmpty(collection)) {
            for (NodeType nodeType : collection) {
                LocationTypeIndexed event = new LocationTypeIndexed(this);
                event.setNodeType(nodeType);
                event.setLocation(location);
                event.setOrchestratorFactory(orchestratorFactory);
                applicationContext.publishEvent(event);
            }
        }
    }

    private void injectWorkSpace(Collection<? extends AbstractToscaType> elements, Orchestrator orchestrator, Location location) {
        for (AbstractToscaType element : elements) {
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
    public Map<Csar, List<Usage>> deleteArchives(Orchestrator orchestrator, Location location) {
        ILocationConfiguratorPlugin configuratorPlugin = getConfiguratorPlugin(location);
        IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestrator);
        List<PluginArchive> pluginArchives = configuratorPlugin.pluginArchives();
        // abort if no archive is exposed by this location
        if (CollectionUtils.isEmpty(pluginArchives)) {
            return null;
        }
        Map<String, List<Location>> allExposedArchivesIds = getAllExposedArchivesIdsExluding(location);
        Map<Csar, List<Usage>> usages = Maps.newHashMap();
        for (PluginArchive pluginArchive : pluginArchives) {
            Csar csar = pluginArchive.getArchive().getArchive();
            List<Location> locationsExposingArchive = allExposedArchivesIds.get(csar.getId());
            LocationArchiveDeleteRequested e = new LocationArchiveDeleteRequested(this);
            e.setCsar(csar);
            e.setLocation(location);
            e.setOrchestratorFactory(orchestratorFactory);
            e.setLocationsExposingArchive(locationsExposingArchive);
            // only delete if no other location exposed this archive
            if (locationsExposingArchive == null) {
                List<Usage> csarUsage = csarService.deleteCsarWithElements(csar);
                if (CollectionUtils.isNotEmpty(csarUsage)) {
                    usages.put(csar, csarUsage);
                }
                e.setDeleted(true);
            } else {
                e.setDeleted(false);
            }
            applicationContext.publishEvent(e);
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

    /**
     * Query for csars that are defined by locations.
     *
     * @return A maps of <csar_id, list of locations that uses the csar>.
     */
    public Map<String, List<Location>> getAllExposedArchivesIdsExluding(Location excludedLocation) {
        // exclude a location from the search
        QueryBuilder query = QueryBuilders.boolQuery()
                .mustNot(QueryBuilders.idsQuery(Location.class.getSimpleName().toLowerCase()).ids(excludedLocation.getId()));
        List<Location> locations = alienDAO.customFindAll(Location.class, query);
        Map<String, List<Location>> archiveIds = Maps.newHashMap();
        if (locations != null) {
            for (Location location : locations) {
                ILocationConfiguratorPlugin configuratorPlugin = getConfiguratorPlugin(location);
                List<PluginArchive> pluginArchives = configuratorPlugin.pluginArchives();
                for (PluginArchive pluginArchive : pluginArchives) {
                    String archiveId = pluginArchive.getArchive().getArchive().getId();
                    List<Location> locationsPerArchive = archiveIds.get(archiveId);
                    if (locationsPerArchive == null) {
                        locationsPerArchive = Lists.newArrayList();
                        archiveIds.put(archiveId, locationsPerArchive);
                    }
                    locationsPerArchive.add(location);
                }
            }
        }

        return archiveIds;
    }
}