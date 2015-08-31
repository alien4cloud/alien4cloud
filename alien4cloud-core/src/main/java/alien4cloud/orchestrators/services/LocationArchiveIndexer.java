package alien4cloud.orchestrators.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.csar.services.CsarService;
import alien4cloud.model.common.Tag;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.paas.PaaSProviderService;
import alien4cloud.tosca.ArchiveIndexer;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;

import com.google.common.collect.Lists;

/**
 * Manage the indexing of TOSCA archives.
 */
@Slf4j
@Component
public class LocationArchiveIndexer {
    @Inject
    private PaaSProviderService paaSProviderService;
    @Inject
    private CsarService csarService;
    @Inject
    private ArchiveIndexer archiveIndexer;

    /**
     * Ensure that plugin archives are indexed, note that by default the archives visibility is not public.
     *
     * @param orchestrator the orchestrator for which to index archives.
     * @param location The location of the orchestrator for which to index archives.
     */
    public void indexArchives(Orchestrator orchestrator, Location location) {
        IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) paaSProviderService.getPaaSProvider(orchestrator.getId());
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorInstance.getConfigurator(location.getInfrastructureType());

        // ensure that the plugin archives for this location are imported in the
        List<PluginArchive> pluginArchives = configuratorPlugin.pluginArchives();

        if (pluginArchives == null) {
            return;
        }

        // index archive here if not already indexed
        for (PluginArchive pluginArchive : pluginArchives) {
            ArchiveRoot archive = pluginArchive.getArchive();
            Csar csar = csarService.getIfExists(archive.getArchive().getName(), archive.getArchive().getVersion());
            if (csar == null) {
                // index the required archive
                indexArchive(pluginArchive, orchestrator, location);
            } else {
                // TODO Link csar and archive elements to the location
                log.debug("Archive {}:{} from plugin {}:{} location {} already exists in the repository and won't be updated.", archive.getArchive().getName(),
                        archive.getArchive().getVersion(), orchestrator.getPluginId(), orchestrator.getPluginBean(), location.getInfrastructureType());
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
            archiveIndexer.importArchive(archive, pluginArchive.getArchiveFilePath(), parsingErrors);
        } catch (CSARVersionAlreadyExistsException e) {
            log.info("Skipping location archive import as the released version already exists in the repository.");
        }
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
}