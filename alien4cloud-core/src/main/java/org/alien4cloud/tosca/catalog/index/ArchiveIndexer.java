package org.alien4cloud.tosca.catalog.index;

import static alien4cloud.dao.FilterUtil.singleKeyFilter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.events.AfterArchiveIndexed;
import org.alien4cloud.tosca.catalog.events.BeforeArchiveIndexed;
import org.alien4cloud.tosca.catalog.repository.ICsarRepositry;
import org.alien4cloud.tosca.editor.services.TopologySubstitutionService;
import org.alien4cloud.tosca.exporter.ArchiveExportService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.components.CSARSource;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.VersionUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Archive indexed is responsible for indexing a TOSCA Cloud Service Archive out of parsing or created from API.
 * </p>
 * <p>
 * It will split the elements of the archive (Tosca Types, Topology, Csar - archive metadata) to multiple objects stored in various indexes.
 * </p>
 * <p>
 * The archive indexer is also responsible for storing or initializing the file repository for the archive and eventually (if the archive is a SNAPSHOT) the
 * local git for editor purpose.
 * </p>
 */
@Slf4j
@Component
public class ArchiveIndexer {
    @Inject
    private ApplicationEventPublisher publisher;
    @Inject
    private ArchiveExportService exportService;
    @Inject
    private ArchiveImageLoader imageLoader;
    @Inject
    private ICsarRepositry archiveRepositry;
    @Inject
    private ICsarService csarService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private IToscaTypeIndexerService indexerService;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private TopologySubstitutionService topologySubstitutionService;
    @Inject
    private IArchiveIndexerAuthorizationFilter archiveIndexerAuthorizationFilter;

    /**
     * Check that a CSAR name/version does not already exists in the repository and eventually throw an AlreadyExistException.
     *
     * @param name The name of the archive.
     * @param version The version of the archive.
     */
    private void ensureUniqueness(String name, String version) {
        long count = csarService.count(singleKeyFilter("version", version), name);
        if (count > 0) {
            throw new AlreadyExistException("CSAR: " + name + ", Version: " + version + " already exists in the repository.");
        }
    }

    /**
     * <p>
     * Import a new empty archive with a topology.
     * </p>
     * <p>
     * Note: this archive is not created from parsing but from alien4cloud API. This service will index the archive and topology as well as initialize the file
     * repository and tosca yaml.
     * </p>
     * <p>
     * This method cannot be used to override a topology, even a SNAPSHOT as any update to a topology from the API MUST be done through the editor.
     * </p>
     * 
     * @param csar The archive to be imported.
     * @param topology The topology to be part of the topology.
     */
    public synchronized void importNewArchive(Csar csar, Topology topology) {
        ArchiveRoot archiveRoot = new ArchiveRoot();
        archiveRoot.setArchive(csar);
        archiveRoot.setTopology(topology);

        // dispatch event before indexing
        publisher.publishEvent(new BeforeArchiveIndexed(this, archiveRoot));

        // Ensure that the archive does not already exists
        ensureUniqueness(csar.getName(), csar.getVersion());
        workflowBuilderService.initWorkflows(workflowBuilderService.buildTopologyContext(topology));

        // generate the initial yaml in a temporary directory
        if (csar.getYamlFilePath() == null) {
            csar.setYamlFilePath("topology.yml");
        }
        String yaml = exportService.getYaml(csar, topology);

        // index the archive and topology
        csarService.save(csar);
        topologyServiceCore.save(topology);
        // Initialize the file repository for the archive
        archiveRepositry.storeCSAR(csar, yaml);

        // dispatch event after indexing
        publisher.publishEvent(new AfterArchiveIndexed(this, archiveRoot));
    }

    /**
     * Import a pre-parsed archive to alien 4 cloud indexed catalog.
     *
     * @param source the source of the archive (alien, orchestrator, upload, git).
     * @param archiveRoot The parsed archive object.
     * @param archivePath The optional path of the archive (should be null if the archive has been java-generated and not parsed).
     * @param parsingErrors The non-null list of parsing errors in which to add errors.
     * @throws CSARUsedInActiveDeployment
     */
    public synchronized void importArchive(final ArchiveRoot archiveRoot, CSARSource source, Path archivePath, List<ParsingError> parsingErrors)
            throws CSARUsedInActiveDeployment {
        archiveIndexerAuthorizationFilter.checkAuthorization(archiveRoot);
        // dispatch event before indexing
        publisher.publishEvent(new BeforeArchiveIndexed(this, archiveRoot));

        String archiveName = archiveRoot.getArchive().getName();
        String archiveVersion = archiveRoot.getArchive().getVersion();
        Csar currentIndexedArchive = csarService.get(archiveName, archiveVersion);
        if (currentIndexedArchive != null) {
            if (Objects.equals(currentIndexedArchive.getWorkspace(), archiveRoot.getArchive().getWorkspace())) {
                if (currentIndexedArchive.getHash() != null && currentIndexedArchive.getHash().equals(archiveRoot.getArchive().getHash())) {
                    // if the archive has not changed do nothing.
                    parsingErrors.add(new ParsingError(ParsingErrorLevel.INFO, ErrorCode.CSAR_ALREADY_INDEXED, "", null,
                            "The archive already exists in alien4cloud with an identical content (SHA-1 on archive content excluding hidden files is identical).",
                            null, archiveName));
                    return;
                }
            } else {
                // If the archive existed in a different workspace then throw error
                parsingErrors.add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.CSAR_ALREADY_EXISTS_IN_ANOTHER_WORKSPACE, "", null,
                        "The archive already exists in alien4cloud in a different workspace.", null, archiveName));
                return;
            }
        }

        // Throw an exception if we are trying to override a released (non SNAPSHOT) version.
        checkNotReleased(currentIndexedArchive);
        // In the current version of alien4cloud we must prevent from overriding an archive that is used in a deployment as we still use catalog information at
        // runtime.
        checkNotUsedInActiveDeployment(currentIndexedArchive);
        // FIXME If the archive already exists but can be indexed we should actually call an editor operation to keep git tracking, or should we just prevent
        // that ?

        // save the archive (before we index and save other data so we can cleanup if anything goes wrong).
        if (source == null) {
            source = CSARSource.OTHER;
        }
        archiveRoot.getArchive().setImportSource(source.name());
        // TODO load transitives dependencies here before saving, as it is not done when parsing
        csarService.save(archiveRoot.getArchive());
        log.debug("Imported archive {}", archiveRoot.getArchive().getId());

        // save the archive in the repository
        archiveRepositry.storeCSAR(archiveRoot.getArchive(), archivePath);
        // manage images before archive storage in the repository
        imageLoader.importImages(archivePath, archiveRoot, parsingErrors);

        // index the archive content in elastic-search
        indexArchiveTypes(archiveName, archiveVersion, archiveRoot.getArchive().getWorkspace(), archiveRoot, currentIndexedArchive);
        indexTopology(archiveRoot, parsingErrors, archiveName, archiveVersion);

        publisher.publishEvent(new AfterArchiveIndexed(this, archiveRoot));
    }

    private void indexTopology(final ArchiveRoot archiveRoot, List<ParsingError> parsingErrors, String archiveName, String archiveVersion) {
        Topology topology = archiveRoot.getTopology();
        if (topology == null || topology.isEmpty()) {
            return;
        }
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
                    public <T extends AbstractToscaType> T findElement(Class<T> clazz, String elementId) {
                        return ToscaContext.get(clazz, elementId);
                    }
                });
        workflowBuilderService.initWorkflows(topologyContext);

        // FIXME query to check if a previous topology exist for this archive name/version/workspace.

        // parsingErrors
        // .add(new ParsingError(ParsingErrorLevel.INFO, ErrorCode.TOPOLOGY_UPDATED, "", null, "A topology template has been updated", null, archiveName));

        parsingErrors.add(
                new ParsingError(ParsingErrorLevel.INFO, ErrorCode.TOPOLOGY_DETECTED, "", null, "A topology template has been detected", null, archiveName));

        topologyServiceCore.saveTopology(topology);
        topologySubstitutionService.updateSubstitutionType(topology, archiveRoot.getArchive());
    }

    private void checkNotUsedInActiveDeployment(Csar csar) throws CSARUsedInActiveDeployment {
        if (csar != null && deploymentService.isArchiveDeployed(csar.getName(), csar.getVersion())) {
            throw new CSARUsedInActiveDeployment("CSAR: " + csar.getName() + ", Version: " + csar.getVersion() + " is used in an active deployment.");
        }
    }

    private void checkNotReleased(Csar archive) {
        if (archive != null && !VersionUtil.isSnapshot(archive.getVersion())) {
            throw new AlreadyExistException("CSAR: " + archive.getName() + ", Version: " + archive.getVersion() + " already exists in the repository.");
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
    private void indexArchiveTypes(String archiveName, String archiveVersion, String workspace, ArchiveRoot root, Csar archive) {
        if (archive != null) {
            // get element from the archive so we get the creation date.
            Map<String, AbstractToscaType> previousElements = indexerService.getArchiveElements(archiveName, archiveVersion);
            prepareForUpdate(root, previousElements);

            // delete the all objects related to the previous archive .
            csarService.deleteCsarContent(archive);
        }

        performIndexing(root);
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

    private void performIndexing(ArchiveRoot root) {
        indexerService.indexInheritableElements(root.getArtifactTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(root.getCapabilityTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(root.getNodeTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(root.getRelationshipTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(root.getDataTypes(), root.getArchive().getDependencies());

        if (root.getLocalImports() != null) {
            for (ArchiveRoot child : root.getLocalImports()) {
                performIndexing(child);
            }
        }
    }
}