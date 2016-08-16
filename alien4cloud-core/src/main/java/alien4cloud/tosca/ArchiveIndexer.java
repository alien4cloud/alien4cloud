package alien4cloud.tosca;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditorRepositoryService;
import org.springframework.stereotype.Component;

import alien4cloud.component.ICSARRepositoryIndexerService;
import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.component.repository.ICsarRepositry;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import alien4cloud.csar.services.CsarService;
import alien4cloud.model.components.*;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.TopologyTemplateVersionService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ToscaParsingUtil;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.VersionUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ArchiveIndexer {
    @Inject
    private ArchiveImageLoader imageLoader;
    @Inject
    private ICsarRepositry archiveRepositry;
    @Inject
    private CsarService csarService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private TopologyTemplateVersionService topologyTemplateVersionService;
    @Inject
    private ICSARRepositoryIndexerService indexerService;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private ICSARRepositorySearchService searchService;
    @Inject
    private EditorRepositoryService repositoryService;

    /**
     * Import a pre-parsed archive to alien 4 cloud indexed catalog.
     *
     * @param source the source of the archive (alien, orchestrator, upload, git).
     * @param archiveRoot The parsed archive object.
     * @param archivePath The optional path of the archive (should be null if the archive has been java-generated and not parsed).
     * @param parsingErrors The non-null list of parsing errors in which to add errors.
     * @throws CSARVersionAlreadyExistsException
     */
    public void importArchive(final ArchiveRoot archiveRoot, CSARSource source, Path archivePath, List<ParsingError> parsingErrors)
            throws CSARVersionAlreadyExistsException {
        String archiveName = archiveRoot.getArchive().getName();
        String archiveVersion = archiveRoot.getArchive().getVersion();
        Csar archive = csarService.getIfExists(archiveName, archiveVersion);

        // Cannot override RELEASED CSAR. FIXME Or RELEASED Topology Template
        if (archive != null && !VersionUtil.isSnapshot(archive.getVersion())) {
            throw new CSARVersionAlreadyExistsException("CSAR: " + archiveName + ", Version: " + archiveVersion + " already exists in the repository.");
        }

        // save the archive (before we index and save other data so we can cleanup if anything goes wrong).
        if (source == null) {
            source = CSARSource.OTHER;
        }
        archiveRoot.getArchive().setImportSource(source.name());
        csarService.save(archiveRoot.getArchive());
        log.debug("Imported archive {}", archiveRoot.getArchive().getId());
        if (archivePath != null) {
            // save the archive in the repository
            archiveRepositry.storeCSAR(archiveName, archiveVersion, archivePath);
            // manage images before archive storage in the repository
            imageLoader.importImages(archivePath, archiveRoot, parsingErrors);
        } // TODO What if else ? should we generate the YAML ?

        // index the archive content in elastic-search
        indexArchiveTypes(archiveName, archiveVersion, archiveRoot, archive != null);

        final Topology topology = archiveRoot.getTopology();
        // if a topology has been added we want to notify the user
        if (topology != null && !topology.isEmpty()) {
            if (archiveRoot.hasToscaTypes()) {
                // The archive contains types, we assume those types are used in the embedded topology so we add the dependency to this CSAR
                CSARDependency selfDependency = new CSARDependency(archiveRoot.getArchive().getName(), archiveRoot.getArchive().getVersion());
                topology.getDependencies().add(selfDependency);
            }

            // init the workflows
            WorkflowsBuilderService.TopologyContext topologyContext = workflowBuilderService
                    .buildCachedTopologyContext(new WorkflowsBuilderService.TopologyContext() {
                        @Override
                        public Topology getTopology() {
                            return topology;
                        }

                        @Override
                        public <T extends IndexedToscaElement> T findElement(Class<T> clazz, String id) {
                            return ToscaParsingUtil.getElementFromArchiveOrDependencies(clazz, id, archiveRoot, searchService);
                        }
                    });
            workflowBuilderService.initWorkflows(topologyContext);

            // TODO: here we should update the topology if it already exists
            // TODO: the name should only contains the archiveName
            TopologyTemplate existingTemplate = topologyServiceCore.searchTopologyTemplateByName(archiveRoot.getArchive().getName());
            String topologyId;
            if (existingTemplate != null) {
                // the topology template already exists
                topology.setDelegateId(existingTemplate.getId());
                topology.setDelegateType(TopologyTemplate.class.getSimpleName().toLowerCase());
                topologyId = topologyServiceCore.saveTopology(topology);
                // now search the version
                TopologyTemplateVersion ttv = topologyTemplateVersionService.searchByDelegateAndVersion(existingTemplate.getId(), archiveVersion);
                if (ttv != null) {
                    // the version exists, we will update it's topology id and delete the old topology
                    topologyTemplateVersionService.changeTopology(ttv, topologyId);
                } else {
                    // we just create a new version
                    topologyTemplateVersionService.createVersion(existingTemplate.getId(), null, archiveVersion, null, topology);
                }
                parsingErrors.add(new ParsingError(ParsingErrorLevel.INFO, ErrorCode.TOPOLOGY_UPDATED, "", null, "A topology template has been updated", null,
                        archiveName));
            } else {
                parsingErrors.add(new ParsingError(ParsingErrorLevel.INFO, ErrorCode.TOPOLOGY_DETECTED, "", null, "A topology template has been detected", null,
                        archiveName));
                topologyServiceCore.createTopologyTemplate(topology, archiveName, archiveRoot.getTopologyTemplateDescription(), archiveVersion);
                topologyId = topology.getId();
            }
            // store the archive for topology edition in case the version is snapshot
            if (VersionUtil.isSnapshot(archiveVersion)) {
                // Copy files from the archive repository to the editor
                try {
                    repositoryService.copyFrom(topologyId, archiveRepositry.getExpandedCSAR(archiveName, archiveVersion));
                } catch (CSARVersionNotFoundException | IOException e) {
                    log.error("Failed to initialize the topology repository", e);
                    // FIXME we should cleanup everything or actually do that before indexing all data.
                }
            }
            topologyServiceCore.updateSubstitutionType(topology);
        }
    }

    /**
     * Index an archive in Alien indexed repository.
     *
     * @param archiveName The name of the archive.
     * @param archiveVersion The version of the archive.
     * @param root The archive root.
     * @param update true if the archive is updated, false if the archive is just indexed.
     */
    private void indexArchiveTypes(String archiveName, String archiveVersion, ArchiveRoot root, boolean update) {
        if (update) {
            // get element from the archive so we get the creation date.
            Map<String, IndexedToscaElement> previousElements = indexerService.getArchiveElements(archiveName, archiveVersion);
            prepareForUpdate(archiveName, archiveVersion, root, previousElements);
            // delete all previous elements and their images
            indexerService.deleteElements(previousElements.values());
        }

        performIndexing(archiveName, archiveVersion, root);
    }

    private void prepareForUpdate(String archiveName, String archiveVersion, ArchiveRoot root, Map<String, IndexedToscaElement> previousElements) {
        updateCreationDates(root.getArtifactTypes(), previousElements);
        updateCreationDates(root.getCapabilityTypes(), previousElements);
        updateCreationDates(root.getNodeTypes(), previousElements);
        updateCreationDates(root.getRelationshipTypes(), previousElements);
        updateCreationDates(root.getDataTypes(), previousElements);

        if (root.getLocalImports() != null) {
            for (ArchiveRoot child : root.getLocalImports()) {
                prepareForUpdate(archiveName, archiveVersion, child, previousElements);
            }
        }
    }

    private void updateCreationDates(Map<String, ? extends IndexedInheritableToscaElement> newElements, Map<String, IndexedToscaElement> previousElements) {
        if (newElements == null) {
            return;
        }
        for (IndexedInheritableToscaElement newElement : newElements.values()) {
            IndexedToscaElement previousElement = previousElements.get(newElement.getId());
            if (previousElement != null) {
                newElement.setCreationDate(previousElement.getCreationDate());
            }
        }
    }

    private void performIndexing(String archiveName, String archiveVersion, ArchiveRoot root) {
        indexerService.indexInheritableElements(archiveName, archiveVersion, root.getArtifactTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(archiveName, archiveVersion, root.getCapabilityTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(archiveName, archiveVersion, root.getNodeTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(archiveName, archiveVersion, root.getRelationshipTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(archiveName, archiveVersion, root.getDataTypes(), root.getArchive().getDependencies());

        if (root.getLocalImports() != null) {
            for (ArchiveRoot child : root.getLocalImports()) {
                performIndexing(archiveName, archiveVersion, child);
            }
        }
    }
}