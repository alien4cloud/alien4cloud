package alien4cloud.tosca;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import alien4cloud.topology.TopologyTemplateService;
import org.alien4cloud.tosca.editor.EditorRepositoryService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.springframework.stereotype.Component;

import alien4cloud.component.ICSARRepositoryIndexerService;
import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.component.repository.ICsarRepositry;
import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.component.repository.exception.CSARVersionNotFoundException;
import alien4cloud.csar.services.CsarService;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.model.components.*;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;
import org.alien4cloud.tosca.model.templates.Topology;
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
    private TopologyTemplateService topologyTemplateService;
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
    @Inject
    private DeploymentService deploymentService;

    /**
     * Import a pre-parsed archive to alien 4 cloud indexed catalog.
     *
     * @param source the source of the archive (alien, orchestrator, upload, git).
     * @param archiveRoot The parsed archive object.
     * @param archivePath The optional path of the archive (should be null if the archive has been java-generated and not parsed).
     * @param parsingErrors The non-null list of parsing errors in which to add errors.
     * @throws CSARVersionAlreadyExistsException
     * @throws CSARUsedInActiveDeployment
     */
    public void importArchive(final ArchiveRoot archiveRoot, CSARSource source, Path archivePath, List<ParsingError> parsingErrors)
            throws CSARVersionAlreadyExistsException, CSARUsedInActiveDeployment {
        String archiveName = archiveRoot.getArchive().getName();
        String archiveVersion = archiveRoot.getArchive().getVersion();
        Csar currentIndexedArchive = csarService.get(archiveName, archiveVersion);
        // if the archive has not changed do nothing.
        if (currentIndexedArchive != null && currentIndexedArchive.getHash() != null
                && currentIndexedArchive.getHash().equals(archiveRoot.getArchive().getHash())) {
            parsingErrors.add(new ParsingError(ParsingErrorLevel.INFO, ErrorCode.CSAR_ALREADY_INDEXED, "", null,
                    "The archive already exists in alien4cloud with an identical content (SHA-1 on archive content excluding hidden files is identical).", null,
                    archiveName));
            return;
        }

        // Cannot override RELEASED CSAR .
        checkNotReleased(currentIndexedArchive);
        // Cannot override a CSAR used in an active deployment
        checkNotUsedInActiveDeployment(currentIndexedArchive);

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
        indexArchiveTypes(archiveName, archiveVersion, archiveRoot, currentIndexedArchive);

        final Topology topology = archiveRoot.getTopology();
        // if a topology has been added we want to notify the user
        if (topology != null && !topology.isEmpty()) {
            if (archiveRoot.hasToscaTypes()) {
                // The archive contains types, we assume those types are used in the embedded topology so we add the dependency to this CSAR
                CSARDependency selfDependency = new CSARDependency(archiveRoot.getArchive().getName(), archiveRoot.getArchive().getVersion(),
                        archiveRoot.getArchive().getHash());
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
                        public <T extends AbstractToscaType> T findElement(Class<T> clazz, String id) {
                            return ToscaParsingUtil.getElementFromArchiveOrDependencies(clazz, id, archiveRoot, searchService);
                        }
                    });
            workflowBuilderService.initWorkflows(topologyContext);

            // TODO: here we should update the topology if it already exists
            // TODO: the name should only contains the archiveName
            TopologyTemplate existingTemplate = topologyTemplateService.getTopologyTemplateByName(archiveRoot.getArchive().getName());
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
                topologyTemplateService.createTopologyTemplate(topology, archiveName, archiveRoot.getTopologyTemplateDescription(), archiveVersion);
                topologyId = topology.getId();
            }
            // store the archive for topology edition in case the version is snapshot
            if (VersionUtil.isSnapshot(archiveVersion)) {
                // Copy files from the archive repository to the editor
                try {
                    repositoryService.copyFrom(topologyId, archiveRepositry.getExpandedCSAR(archiveName, archiveVersion));
                } catch (CSARVersionNotFoundException | IOException e) {
                    log.error("Failed to initialize the topology repository", e);
                }
            }
            topologyServiceCore.updateSubstitutionType(topology);
        }
    }

    private void checkNotUsedInActiveDeployment(Csar csar) throws CSARUsedInActiveDeployment {
        if (csar != null && deploymentService.isArchiveDeployed(csar.getName(), csar.getVersion())) {
            throw new CSARUsedInActiveDeployment("CSAR: " + csar.getName() + ", Version: " + csar.getVersion() + " is used in an active deployment.");
        }
    }

    private void checkNotReleased(Csar archive) throws CSARVersionAlreadyExistsException {
        if (archive != null && !VersionUtil.isSnapshot(archive.getVersion())) {
            throw new CSARVersionAlreadyExistsException(
                    "CSAR: " + archive.getName() + ", Version: " + archive.getVersion() + " already exists in the repository.");
        }
    }

    /**
     * Index an archive in Alien indexed repository.
     *
     * @param archiveName The name of the archive.
     * @param archiveVersion The version of the archive.
     * @param root The archive root.
     * @param archive The previous archive that must be replaced if any.
     */
    private void indexArchiveTypes(String archiveName, String archiveVersion, ArchiveRoot root, Csar archive) {
        if (archive != null) {
            // get element from the archive so we get the creation date.
            Map<String, AbstractToscaType> previousElements = indexerService.getArchiveElements(archiveName, archiveVersion);
            prepareForUpdate(root, previousElements);

            // delete the previous archive including all types etc.
            csarService.forceDeleteCsar(archive.getId());
        }

        performIndexing(archiveName, archiveVersion, root);
    }

    private void prepareForUpdate(ArchiveRoot root, Map<String, AbstractToscaType> previousElements) {
        updateCreationDates(root.getArtifactTypes(), previousElements);
        updateCreationDates(root.getCapabilityTypes(), previousElements);
        updateCreationDates(root.getNodeTypes(), previousElements);
        updateCreationDates(root.getRelationshipTypes(), previousElements);
        updateCreationDates(root.getDataTypes(), previousElements);

        if (root.getLocalImports() != null) {
            for (ArchiveRoot child : root.getLocalImports()) {
                prepareForUpdate(child, previousElements);
            }
        }
    }

    private void updateCreationDates(Map<String, ? extends AbstractInheritableToscaType> newElements, Map<String, AbstractToscaType> previousElements) {
        if (newElements == null) {
            return;
        }
        for (AbstractInheritableToscaType newElement : newElements.values()) {
            AbstractToscaType previousElement = previousElements.get(newElement.getId());
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