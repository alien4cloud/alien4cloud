package org.alien4cloud.tosca.catalog.index;

import alien4cloud.common.MetaPropertiesService;
import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.component.repository.exception.ToscaTypeAlreadyDefinedInOtherCSAR;
import alien4cloud.dao.FilterUtil;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.common.IMetaProperties;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.common.MetaPropertyTarget;
import alien4cloud.model.common.Tag;
import alien4cloud.model.components.CSARSource;
import alien4cloud.paas.wf.TopologyContext;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.services.ConstraintPropertyService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.catalog.events.AfterArchiveIndexed;
import org.alien4cloud.tosca.catalog.events.BeforeArchiveIndexed;
import org.alien4cloud.tosca.catalog.repository.ICsarRepositry;
import org.alien4cloud.tosca.editor.services.TopologySubstitutionService;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.exporter.ArchiveExportService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static alien4cloud.utils.AlienUtils.safe;

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

    /**
     * If this prefix is found in TOSCA metadata name, A4C will try to find and feed the corresponding meta-property for NodeType.
     */
    public static final String A4C_METAPROPERTY_PREFIX = "A4C_META_";

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ApplicationEventPublisher publisher;
    @Inject
    private ArchiveExportService exportService;
    @Inject
    private ArchiveImageLoader imageLoader;
    @Inject
    private ICsarRepositry archiveRepositry;
    @Inject
    private CsarService csarService;
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
    @Inject
    private MetaPropertiesService metaPropertiesService;

    @Value("${features.archive_indexer_lock_used_archive:#{true}}")
    private boolean lockUsedArchive;

    /**
     * Check that a CSAR name/version does not already exists in the repository and eventually throw an AlreadyExistException.
     *
     * @param name The name of the archive.
     * @param version The version of the archive.
     */
    public void ensureUniqueness(String name, String version) {
        csarService.ensureUniqueness(name, version);
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
     * @param topologyPath if the new topology must be created inside this directory to have all its artifacts
     */
    @SneakyThrows
    public synchronized void importNewArchive(Csar csar, Topology topology, Path topologyPath) {
        ArchiveRoot archiveRoot = new ArchiveRoot();
        archiveRoot.setArchive(csar);
        archiveRoot.setTopology(topology);
        csar.setHasTopology(true);

        // dispatch event before indexing
        publisher.publishEvent(new BeforeArchiveIndexed(this, archiveRoot));

        // Ensure that the archive does not already exists
        ensureUniqueness(csar.getName(), csar.getVersion());

        // generate the initial yaml in a temporary directory
        if (csar.getYamlFilePath() == null) {
            csar.setYamlFilePath("topology.yml");
        }
        String yaml = exportService.getYaml(csar, topology);

        // synch the dependencies before indexing
        csar.setDependencies(topology.getDependencies());

        // index the archive and topology
        csarService.save(csar);
        topologyServiceCore.save(topology);
        // Initialize the file repository for the archive
        if (topologyPath == null) {
            // This is an empty topology without artifacts
            archiveRepositry.storeCSAR(csar, yaml);
        } else {
            Files.write(topologyPath.resolve(csar.getYamlFilePath()), yaml.getBytes(Charset.forName("UTF-8")));
            archiveRepositry.storeCSAR(csar, topologyPath);
        }
        topologySubstitutionService.updateSubstitutionType(topology, archiveRoot.getArchive());
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
            throws CSARUsedInActiveDeployment, ToscaTypeAlreadyDefinedInOtherCSAR {
        archiveIndexerAuthorizationFilter.checkAuthorization(archiveRoot);
        String archiveName = archiveRoot.getArchive().getName();
        String archiveVersion = archiveRoot.getArchive().getVersion();
        Csar currentIndexedArchive = csarService.get(archiveName, archiveVersion);

        if (currentIndexedArchive != null) {
            if (Objects.equals(currentIndexedArchive.getWorkspace(), archiveRoot.getArchive().getWorkspace())) {
                if (currentIndexedArchive.getHash() != null && currentIndexedArchive.getHash().equals(archiveRoot.getArchive().getHash())) {
                    // if the archive has not changed do nothing.
                    parsingErrors
                            .add(new ParsingError(
                                    ParsingErrorLevel.INFO,
                                    ErrorCode.CSAR_ALREADY_INDEXED,
                                    "",
                                    null,
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

        // dispatch event before indexing
        publisher.publishEvent(new BeforeArchiveIndexed(this, archiveRoot));

        // Throw an exception if we are trying to override a released (non SNAPSHOT) version.
        checkNotReleased(currentIndexedArchive);
        // In the current version of alien4cloud we must prevent from overriding an archive that is used in a deployment as we still use catalog information at
        // runtime.
        if (lockUsedArchive) {
            checkNotUsedInActiveDeployment(currentIndexedArchive);
        }
        // FIXME If the archive already exists but can be indexed we should actually call an editor operation to keep git tracking, or should we just prevent
        // that ?

        checkIfToscaTypesAreDefinedInOtherArchive(archiveRoot);

        // save the archive (before we index and save other data so we can cleanup if anything goes wrong).
        if (source == null) {
            source = CSARSource.OTHER;
        }
        archiveRoot.getArchive().setImportSource(source.name());
        archiveRoot.getArchive().setHasTopology(archiveRoot.hasToscaTopologyTemplate() && !archiveRoot.getTopology().isEmpty());
        archiveRoot.getArchive().setNodeTypesCount(archiveRoot.getNodeTypes().size());
        // TODO load transitives dependencies here before saving, as it is not done when parsing
        csarService.save(archiveRoot.getArchive());
        log.debug("Imported archive {}", archiveRoot.getArchive().getId());

        // save the archive in the repository
        archiveRepositry.storeCSAR(archiveRoot.getArchive(), archivePath);
        // manage images before archive storage in the repository
        imageLoader.importImages(archivePath, archiveRoot, parsingErrors);

        Map<String, MetaPropConfiguration> metapropsNames = metaPropertiesService.getMetaPropConfigurationsByName(MetaPropertyTarget.COMPONENT);

        // index the archive content in elastic-search
        indexArchiveTypes(archiveName, archiveVersion, archiveRoot.getArchive().getWorkspace(), archiveRoot, currentIndexedArchive, metapropsNames);
        indexTopology(archiveRoot, parsingErrors, archiveName, archiveVersion);

        publisher.publishEvent(new AfterArchiveIndexed(this, archiveRoot));
    }

    /**
     * Fail if at least one tosca type defined in the archive is already define in an other archive.
     *
     * @param archiveRoot
     * @throws ToscaTypeAlreadyDefinedInOtherCSAR
     */
    private void checkIfToscaTypesAreDefinedInOtherArchive(final ArchiveRoot archiveRoot) throws ToscaTypeAlreadyDefinedInOtherCSAR {
        failIfOneToscaTypesIsDefinedInOtherArchive(archiveRoot.getNodeTypes());
        failIfOneToscaTypesIsDefinedInOtherArchive(archiveRoot.getRelationshipTypes());
        failIfOneToscaTypesIsDefinedInOtherArchive(archiveRoot.getCapabilityTypes());
        failIfOneToscaTypesIsDefinedInOtherArchive(archiveRoot.getArtifactTypes());
        failIfOneToscaTypesIsDefinedInOtherArchive(archiveRoot.getDataTypes());
    }

    private void failIfOneToscaTypesIsDefinedInOtherArchive(Map<String, ? extends AbstractToscaType> toscaTypes) throws ToscaTypeAlreadyDefinedInOtherCSAR {
        if (toscaTypes == null) {
            return;
        }
        for (AbstractToscaType toscaType : toscaTypes.values()) {
            failIfToscaTypeIsDefinedInOtherArchive(toscaType);
        }
    }

    private void failIfToscaTypeIsDefinedInOtherArchive(AbstractToscaType toscaType) throws ToscaTypeAlreadyDefinedInOtherCSAR {
        if (toscaType == null) {
            return;
        }
        AbstractToscaType indexedNodeType = alienDAO.buildQuery(AbstractToscaType.class)
                .setFilters(FilterUtil.singleKeyFilter("elementId", toscaType.getElementId())).prepareSearch().find();
        if (indexedNodeType != null && !toscaType.getArchiveName().equals(indexedNodeType.getArchiveName())) {
            throw new ToscaTypeAlreadyDefinedInOtherCSAR("Tosca type: " + toscaType.getElementId() + ", version: " + toscaType.getArchiveVersion()
                    + " is already defined in archive " + indexedNodeType.getArchiveName() + ":" + indexedNodeType.getArchiveVersion());
        }
    }

    private void manageTopologyMetaproperties(Topology topology) {
        Map<String, MetaPropConfiguration> metapropsNames = metaPropertiesService.getMetaPropConfigurationsByName(MetaPropertyTarget.TOPOLOGY);
        feedA4CMetaproperties(topology, topology.getTags(), metapropsNames);
    }

    private void indexTopology(final ArchiveRoot archiveRoot, List<ParsingError> parsingErrors, String archiveName, String archiveVersion) {
        Topology topology = archiveRoot.getTopology();
        if (topology == null || topology.isEmpty()) {
            return;
        }

        // merge archive tags in topology tags
        if (archiveRoot.getArchive().getTags() != null && !archiveRoot.getArchive().getTags().isEmpty()) {
            if (topology.getTags() == null) {
                topology.setTags(Lists.newArrayList());
            }
            topology.getTags().addAll(archiveRoot.getArchive().getTags());
        }
        manageTopologyMetaproperties(topology);
        if (StringUtils.isEmpty(topology.getDescription()) && StringUtils.isNotEmpty(archiveRoot.getArchive().getDescription())) {
            topology.setDescription(archiveRoot.getArchive().getDescription());
        }

        if (archiveRoot.hasToscaTypes()) {
            // The archive contains types, we assume those types are used in the embedded topology so we add the dependency to this CSAR
            CSARDependency selfDependency = new CSARDependency(archiveRoot.getArchive().getName(), archiveRoot.getArchive().getVersion(), archiveRoot
                    .getArchive().getHash());
            topology.getDependencies().add(selfDependency);
        }

        // init the workflows
        TopologyContext topologyContext = workflowBuilderService.buildCachedTopologyContext(new TopologyContext() {
            @Override
            public String getDSLVersion() {
                return archiveRoot.getArchive().getToscaDefinitionsVersion();
            }

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

        parsingErrors.add(new ParsingError(ParsingErrorLevel.INFO, ErrorCode.TOPOLOGY_DETECTED, "", null, "A topology template has been detected", null,
                archiveName));

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
    private void indexArchiveTypes(String archiveName, String archiveVersion, String workspace, ArchiveRoot root, Csar archive, Map<String, MetaPropConfiguration> metapropsNames) {
        if (archive != null) {
            // get element from the archive so we get the creation date.
            Map<String, AbstractToscaType> previousElements = indexerService.getArchiveElements(archiveName, archiveVersion);
            prepareForUpdate(root, previousElements, metapropsNames);

            // delete the all objects related to the previous archive .
            csarService.deleteCsarContent(archive);
        }

        performIndexing(root, metapropsNames);
    }

    private void prepareForUpdate(ArchiveRoot root, Map<String, AbstractToscaType> previousElements,Map<String, MetaPropConfiguration> metapropsNames) {
        updateCreationDates(root.getArtifactTypes(), previousElements);
        updateCreationDates(root.getCapabilityTypes(), previousElements);
        updateCreationDates(root.getNodeTypes(), previousElements);
        updateComponentMetaProperties(root.getNodeTypes(), previousElements, metapropsNames);
        updateCreationDates(root.getRelationshipTypes(), previousElements);
        updateCreationDates(root.getDataTypes(), previousElements);
        updateCreationDates(root.getPolicyTypes(), previousElements);

        if (root.getLocalImports() != null) {
            for (ArchiveRoot child : root.getLocalImports()) {
                prepareForUpdate(child, previousElements, metapropsNames);
            }
        }
    }

    private void feedA4CMetaproperties(IMetaProperties newElement, List<Tag> tags, Map<String, MetaPropConfiguration> metapropsByNames) {
        if (tags == null) {
            return;
        }
        Map<String, String> metaProperties = newElement.getMetaProperties();
        if (metaProperties == null) {
            metaProperties = Maps.newHashMap();
            newElement.setMetaProperties(metaProperties);
        }
        Set<String> tagsToRemove = Sets.newHashSet();

        Iterator<Tag> tagIterator = tags.iterator();
        while (tagIterator.hasNext()) {
            Tag tag = tagIterator.next();
            if (tag.getName().startsWith(A4C_METAPROPERTY_PREFIX)) {
                String metapropertyName = tag.getName().substring(A4C_METAPROPERTY_PREFIX.length());
                MetaPropConfiguration metaPropConfig = metapropsByNames.get(metapropertyName);
                if (metaPropConfig != null) {
                    // validate tag value using meta prop constraints
                    try {
                        ConstraintPropertyService.checkPropertyConstraint(metaPropConfig.getId(), tag.getValue(), metaPropConfig);
                        metaProperties.put(metaPropConfig.getId(), tag.getValue());
                        tagIterator.remove();
                    } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                        // TODO: manage error
                        // for the moment the error is ignored, but the meta-property is not set and the
                        // tag not removed, so the user can easily guess that something gone wrong ...
                    } catch (ConstraintViolationException e) {
                        // TODO: manage error
                    }
                }
            }
        }
    }

    private void updateComponentMetaProperties(Map<String, NodeType> newElements, Map<String, AbstractToscaType> previousElements, Map<String, MetaPropConfiguration> metapropsNames) {
        if (newElements == null) {
            return;
        }

//        Map<String, MetaPropConfiguration> metapropsNames = metaPropertiesService.getMetaPropConfigurationsByName(MetaPropertyTarget.COMPONENT);

        for (NodeType newElement : newElements.values()) {
            feedA4CMetaproperties(newElement, newElement.getTags(), metapropsNames);
            // now copy meta props from previous type if exists
            Map<String, String> metaProperties = newElement.getMetaProperties();
            if (metaProperties == null) {
                metaProperties = Maps.newHashMap();
                newElement.setMetaProperties(metaProperties);
            }
            AbstractToscaType previousElement = previousElements.get(newElement.getId());
            if (previousElement != null && previousElement instanceof NodeType) {
                for (Map.Entry<String, String> previousMetaPropsEntry : safe(((NodeType) previousElement).getMetaProperties()).entrySet()) {
                    if (!metaProperties.containsKey(previousMetaPropsEntry.getKey())) {
                        metaProperties.put(previousMetaPropsEntry.getKey(), previousMetaPropsEntry.getValue());
                    }
                }
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

    private void performIndexing(ArchiveRoot root, Map<String, MetaPropConfiguration> metapropsNames) {
        indexerService.indexInheritableElements(root.getArtifactTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(root.getCapabilityTypes(), root.getArchive().getDependencies());
        root.getNodeTypes().forEach((id, nodeType) -> {
            feedA4CMetaproperties(nodeType, nodeType.getTags(), metapropsNames); }
        );
        indexerService.indexInheritableElements(root.getNodeTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(root.getRelationshipTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(root.getDataTypes(), root.getArchive().getDependencies());
        indexerService.indexInheritableElements(root.getPolicyTypes(), root.getArchive().getDependencies());

        if (root.getLocalImports() != null) {
            for (ArchiveRoot child : root.getLocalImports()) {
                performIndexing(child, metapropsNames);
            }
        }
    }
}