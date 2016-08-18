package alien4cloud.topology;

import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.component.repository.IFileRepository;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.exception.VersionConflictException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.components.*;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.model.topology.*;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.Role;
import alien4cloud.security.model.User;
import alien4cloud.topology.exception.UpdateTopologyException;
import alien4cloud.topology.task.SuggestionsTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.tosca.container.ToscaTypeLoader;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.serializer.VelocityUtil;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.cache.CachedFinder;
import alien4cloud.utils.cache.IFinder;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.mapping.FilterValuesStrategy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TopologyService {

    @Resource
    private CSARRepositorySearchService csarRepoSearchService;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private ApplicationService appService;

    @Resource
    private CsarService csarService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private ApplicationVersionService applicationVersionService;

    @Resource
    private TopologyTemplateVersionService topologyTemplateVersionService;

    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    public static final Pattern NODE_NAME_PATTERN = Pattern.compile("^\\w+$");
    public static final Pattern NODE_NAME_REPLACE_PATTERN = Pattern.compile("\\W");
    @Resource
    public IFileRepository artifactRepository;

    public static final String NODE_NAME_REGEX = "^\\w+$";

    private ToscaTypeLoader initializeTypeLoader(Topology topology) {
        ToscaTypeLoader loader = new ToscaTypeLoader(csarService);
        CachedFinder<Csar> finder = buildCaheFinder();
        Map<String, IndexedNodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, false);
        Map<String, IndexedRelationshipType> relationshipTypes = topologyServiceCore.getIndexedRelationshipTypesFromTopology(topology);
        if (topology.getNodeTemplates() != null) {
            for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
                IndexedNodeType nodeType = nodeTypes.get(nodeTemplate.getType());
                loader.loadType(nodeTemplate.getType(), buildDependencyBean(nodeType.getArchiveName(), nodeType.getArchiveVersion(), finder));
                if (nodeTemplate.getRelationships() != null) {
                    for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                        IndexedRelationshipType relationshipType = relationshipTypes.get(relationshipTemplate.getType());
                        loader.loadType(relationshipTemplate.getType(),
                                buildDependencyBean(relationshipType.getArchiveName(), relationshipType.getArchiveVersion(), finder));
                    }
                }
            }
        }
        if (topology.getSubstitutionMapping() != null && topology.getSubstitutionMapping().getSubstitutionType() != null) {
            IndexedNodeType substitutionType = topology.getSubstitutionMapping().getSubstitutionType();
            loader.loadType(substitutionType.getElementId(),
                    buildDependencyBean(substitutionType.getArchiveName(), substitutionType.getArchiveVersion(), finder));
        }
        return loader;
    }

    /**
     * Get a map of all capability types defined in the given node types.
     *
     * @param nodeTypes The collection of node types for which to get capabilities.
     * @param dependencies The dependencies in which to look for capabilities.
     * @return A map of capability types defined in the given node types.
     */
    public Map<String, IndexedCapabilityType> getIndexedCapabilityTypes(Collection<IndexedNodeType> nodeTypes, Collection<CSARDependency> dependencies) {
        Map<String, IndexedCapabilityType> capabilityTypes = Maps.newHashMap();
        for (IndexedNodeType nodeType : nodeTypes) {
            if (nodeType.getCapabilities() != null) {
                for (CapabilityDefinition capabilityDefinition : nodeType.getCapabilities()) {
                    IndexedCapabilityType capabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class,
                            capabilityDefinition.getType(), dependencies);
                    capabilityTypes.put(capabilityDefinition.getType(), capabilityType);
                }
            }
        }
        return capabilityTypes;
    }

    /**
     * Find replacements nodes for a node template
     *
     * @param nodeTemplateName the node to search for replacements
     * @param topology the topology
     * @return all possible replacement types for this node
     */
    @SneakyThrows(IOException.class)
    public IndexedNodeType[] findReplacementForNode(String nodeTemplateName, Topology topology) {
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        Map<String, Map<String, Set<String>>> nodeTemplatesToFilters = Maps.newHashMap();
        Entry<String, NodeTemplate> nodeTempEntry = Maps.immutableEntry(nodeTemplateName, nodeTemplate);
        IndexedNodeType indexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                topology.getDependencies());
        processNodeTemplate(topology, nodeTempEntry, nodeTemplatesToFilters);
        List<SuggestionsTask> topoTasks = searchForNodeTypes(nodeTemplatesToFilters,
                MapUtil.newHashMap(new String[] { nodeTemplateName }, new IndexedNodeType[] { indexedNodeType }));

        if (CollectionUtils.isEmpty(topoTasks)) {
            return null;
        }
        return topoTasks.get(0).getSuggestedNodeTypes();
    }

    private void addFilters(String nodeTempName, String filterKey, String filterValueToAdd, Map<String, Map<String, Set<String>>> nodeTemplatesToFilters) {
        Map<String, Set<String>> filters = nodeTemplatesToFilters.get(nodeTempName);
        if (filters == null) {
            filters = Maps.newHashMap();
        }
        Set<String> filterValues = filters.get(filterKey);
        if (filterValues == null) {
            filterValues = Sets.newHashSet();
        }

        filterValues.add(filterValueToAdd);
        filters.put(filterKey, filterValues);
        nodeTemplatesToFilters.put(nodeTempName, filters);
    }

    /**
     * Process a node template to retrieve filters for node replacements search.
     *
     * TODO cleanup this method, better return a filter for node rather than adding it to a parameter list.
     *
     * @param topology The topology for which to search filters.
     * @param nodeTempEntry The node template for which to find replacement filter.
     * @param nodeTemplatesToFilters The map of filters in which to add the filter for the new Node.
     */
    public void processNodeTemplate(final Topology topology, final Entry<String, NodeTemplate> nodeTempEntry,
            Map<String, Map<String, Set<String>>> nodeTemplatesToFilters) {
        String capabilityFilterKey = "capabilities.type";
        String requirementFilterKey = "requirements.type";
        NodeTemplate template = nodeTempEntry.getValue();
        Map<String, RelationshipTemplate> relTemplates = template.getRelationships();

        nodeTemplatesToFilters.put(nodeTempEntry.getKey(), null);

        // process the node template source of relationships
        if (relTemplates != null && !relTemplates.isEmpty()) {
            for (RelationshipTemplate relationshipTemplate : relTemplates.values()) {
                addFilters(nodeTempEntry.getKey(), requirementFilterKey, relationshipTemplate.getRequirementType(), nodeTemplatesToFilters);
            }
        }

        // process the node template target of relationships
        List<RelationshipTemplate> relTemplatesTargetRelated = topologyServiceCore.getTargetRelatedRelatonshipsTemplate(nodeTempEntry.getKey(),
                topology.getNodeTemplates());
        for (RelationshipTemplate relationshipTemplate : relTemplatesTargetRelated) {
            addFilters(nodeTempEntry.getKey(), capabilityFilterKey, relationshipTemplate.getRequirementType(), nodeTemplatesToFilters);
        }

    }

    private IndexedNodeType[] getIndexedNodeTypesFromSearchResponse(final GetMultipleDataResult<IndexedNodeType> searchResult,
            final IndexedNodeType toExcludeIndexedNodeType) throws IOException {
        IndexedNodeType[] toReturnArray = null;
        for (int j = 0; j < searchResult.getData().length; j++) {
            IndexedNodeType nodeType = searchResult.getData()[j];
            if (toExcludeIndexedNodeType == null || !nodeType.getId().equals(toExcludeIndexedNodeType.getId())) {
                toReturnArray = ArrayUtils.add(toReturnArray, nodeType);
            }
        }
        return toReturnArray;
    }

    /**
     * Search for nodeTypes given some filters. Apply AND filter strategy when multiple values for a filter key.
     */
    public List<SuggestionsTask> searchForNodeTypes(Map<String, Map<String, Set<String>>> nodeTemplatesToFilters,
            Map<String, IndexedNodeType> toExcludeIndexedNodeTypes) throws IOException {
        if (nodeTemplatesToFilters == null || nodeTemplatesToFilters.isEmpty()) {
            return null;
        }
        List<SuggestionsTask> toReturnTasks = Lists.newArrayList();
        for (Map.Entry<String, Map<String, Set<String>>> nodeTemplatesToFiltersEntry : nodeTemplatesToFilters.entrySet()) {
            Map<String, String[]> formattedFilters = Maps.newHashMap();
            Map<String, FilterValuesStrategy> filterValueStrategy = Maps.newHashMap();
            IndexedNodeType[] data = null;
            if (nodeTemplatesToFiltersEntry.getValue() != null) {
                for (Map.Entry<String, Set<String>> filterEntry : nodeTemplatesToFiltersEntry.getValue().entrySet()) {
                    formattedFilters.put(filterEntry.getKey(), filterEntry.getValue().toArray(new String[filterEntry.getValue().size()]));
                    // AND strategy if multiple values
                    filterValueStrategy.put(filterEntry.getKey(), FilterValuesStrategy.AND);
                }

                // retrieve only non abstract components
                formattedFilters.put("abstract", ArrayUtils.toArray("false"));

                GetMultipleDataResult<IndexedNodeType> searchResult = alienDAO.search(IndexedNodeType.class, null, formattedFilters, filterValueStrategy, 20);
                data = getIndexedNodeTypesFromSearchResponse(searchResult, toExcludeIndexedNodeTypes.get(nodeTemplatesToFiltersEntry.getKey()));
            }
            TaskCode taskCode = data == null || data.length < 1 ? TaskCode.IMPLEMENT : TaskCode.REPLACE;
            SuggestionsTask task = new SuggestionsTask();
            task.setNodeTemplateName(nodeTemplatesToFiltersEntry.getKey());
            task.setComponent(toExcludeIndexedNodeTypes.get(nodeTemplatesToFiltersEntry.getKey()));
            task.setCode(taskCode);
            task.setSuggestedNodeTypes(data);
            toReturnTasks.add(task);
        }

        return toReturnTasks;
    }

    /**
     * Check that the user has enough rights for a given topology.
     *
     * @param topology The topology for which to check roles.
     * @param applicationRoles The roles required to edit the topology for an application.
     */
    public void checkAuthorizations(Topology topology, ApplicationRole... applicationRoles) {
        if (topology.getDelegateType().equals(Application.class.getSimpleName().toLowerCase())) {
            String applicationId = topology.getDelegateId();
            Application application = appService.getOrFail(applicationId);
            AuthorizationUtil.checkAuthorizationForApplication(application, applicationRoles);
        } else {
            AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);
        }
    }

    /**
     * Check that the current user can update the given topology.
     *
     * @param topology The topology that is subject to being updated.
     */
    public void checkEditionAuthorizations(Topology topology) {
        checkAuthorizations(topology, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS);
    }

    /**
     * Create a {@link TopologyDTO} from a topology by fetching node types, relationship types and capability types used in the topology.
     *
     * @param topology The topology for which to create a DTO.
     * @return The {@link TopologyDTO} that contains the given topology
     */
    @Deprecated
    public TopologyDTO buildTopologyDTO(Topology topology) {
        Map<String, IndexedNodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, false);
        Map<String, IndexedRelationshipType> relationshipTypes = topologyServiceCore.getIndexedRelationshipTypesFromTopology(topology);
        Map<String, IndexedCapabilityType> capabilityTypes = getIndexedCapabilityTypes(nodeTypes.values(), topology.getDependencies());
        Map<String, Map<String, Set<String>>> outputCapabilityProperties = topology.getOutputCapabilityProperties();
        Map<String, IndexedDataType> dataTypes = getDataTypes(topology, nodeTypes, relationshipTypes, capabilityTypes);
        return new TopologyDTO(topology, nodeTypes, relationshipTypes, capabilityTypes, outputCapabilityProperties, dataTypes);
    }

    private Map<String, IndexedDataType> getDataTypes(Topology topology, Map<String, IndexedNodeType> nodeTypes,
            Map<String, IndexedRelationshipType> relationshipTypes, Map<String, IndexedCapabilityType> capabilityTypes) {
        Map<String, IndexedDataType> indexedDataTypes = Maps.newHashMap();
        indexedDataTypes = fillDataTypes(topology, indexedDataTypes, nodeTypes);
        indexedDataTypes = fillDataTypes(topology, indexedDataTypes, relationshipTypes);
        indexedDataTypes = fillDataTypes(topology, indexedDataTypes, capabilityTypes);
        return indexedDataTypes;
    }

    private <T extends IndexedInheritableToscaElement> Map<String, IndexedDataType> fillDataTypes(Topology topology,
            Map<String, IndexedDataType> indexedDataTypes, Map<String, T> elements) {
        for (IndexedInheritableToscaElement indexedNodeType : elements.values()) {
            if (indexedNodeType.getProperties() != null) {
                for (PropertyDefinition pd : indexedNodeType.getProperties().values()) {
                    String type = pd.getType();
                    if (ToscaType.isPrimitive(type) || indexedDataTypes.containsKey(type)) {
                        continue;
                    }
                    IndexedDataType dataType = csarRepoSearchService.getElementInDependencies(IndexedDataType.class, type, topology.getDependencies());
                    if (dataType == null) {
                        dataType = csarRepoSearchService.getElementInDependencies(PrimitiveIndexedDataType.class, type, topology.getDependencies());
                    }
                    indexedDataTypes.put(type, dataType);
                }
            }
        }
        return indexedDataTypes;
    }

    /**
     * Build a node template
     *
     * @param dependencies the dependencies on which new node will be constructed
     * @param indexedNodeType the type of the node
     * @param templateToMerge the template that can be used to merge into the new node template
     * @return new constructed node template
     */
    public NodeTemplate buildNodeTemplate(Set<CSARDependency> dependencies, IndexedNodeType indexedNodeType, NodeTemplate templateToMerge) {
        return topologyServiceCore.buildNodeTemplate(dependencies, indexedNodeType, templateToMerge);
    }

    private CSARDependency getDependencyWithName(Topology topology, String archiveName) {
        for (CSARDependency dependency : topology.getDependencies()) {
            if (dependency.getName().equals(archiveName)) {
                return dependency;
            }
        }
        return null;
    }

    /**
     * Load a type into the topology (add dependency for this new type, upgrade if necessary ...)
     *
     * @param topology the topology
     * @param element the element to load
     * @param <T> tosca element type
     * @return The real loaded element if element given in argument is from older archive than topology's dependencies
     */
    @SuppressWarnings("unchecked")
    public <T extends IndexedToscaElement> T loadType(Topology topology, T element) {
        String type = element.getElementId();
        String archiveName = element.getArchiveName();
        String archiveVersion = element.getArchiveVersion();
        CSARDependency topologyDependency = getDependencyWithName(topology, archiveName);
        CSARDependency toLoadDependency = topologyDependency;
        CachedFinder<Csar> finder = buildCaheFinder();
        if (topologyDependency != null) {
            int comparisonResult = VersionUtil.compare(archiveVersion, topologyDependency.getVersion());
            if (comparisonResult > 0) {
                // Dependency of the type is more recent, try to upgrade the topology
                toLoadDependency = buildDependencyBean(archiveName, archiveVersion, finder);
                topology.getDependencies().add(toLoadDependency);
                topology.getDependencies().remove(topologyDependency);
                Map<String, IndexedNodeType> nodeTypes;
                try {
                    nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, false);
                    topologyServiceCore.getIndexedRelationshipTypesFromTopology(topology);
                } catch (NotFoundException e) {
                    throw new VersionConflictException("Version conflict, cannot add archive [" + archiveName + ":" + archiveVersion
                            + "], upgrade of the topology to this archive from version [" + topologyDependency.getVersion() + "] failed", e);
                }
                // Try to upgrade existing nodes
                Map<String, NodeTemplate> newNodeTemplates = Maps.newHashMap();
                Map<String, NodeTemplate> existingNodeTemplates = topology.getNodeTemplates();
                if (existingNodeTemplates != null) {
                    for (Entry<String, NodeTemplate> nodeTemplateEntry : existingNodeTemplates.entrySet()) {
                        NodeTemplate newNodeTemplate = buildNodeTemplate(topology.getDependencies(), nodeTypes.get(nodeTemplateEntry.getValue().getType()),
                                nodeTemplateEntry.getValue());
                        newNodeTemplate.setName(nodeTemplateEntry.getKey());
                        newNodeTemplates.put(nodeTemplateEntry.getKey(), newNodeTemplate);
                    }
                    topology.setNodeTemplates(newNodeTemplates);
                }
            } else if (comparisonResult < 0) {
                // Dependency of the topology is more recent, try to upgrade the dependency of the type
                element = csarRepoSearchService.getElementInDependencies((Class<T>) element.getClass(), element.getElementId(), topology.getDependencies());
                toLoadDependency = topologyDependency;
            }
        } else {
            // the type is not yet loaded
            toLoadDependency = buildDependencyBean(archiveName, archiveVersion, finder);
        }
        // FIXME Transitive dependencies could change here and thus types be affected ?
        ToscaTypeLoader typeLoader = initializeTypeLoader(topology);
        typeLoader.loadType(type, toLoadDependency);
        for (CSARDependency updatedDependency : typeLoader.getLoadedDependencies()) {
            ToscaContext.get().updateDependency(updatedDependency);
        }
        topology.setDependencies(typeLoader.getLoadedDependencies());
        return element;
    }

    private CSARDependency buildDependencyBean(String name, String version, IFinder<Csar> finder) {
        CSARDependency newDependency = new CSARDependency(name, version);
        Csar csar = new Csar(name, version);
        csar = finder.find(Csar.class, csar.getId());
        csar = csarService.findByIds(FetchContext.SUMMARY, csar.getId()).get(csar.getId());
        if (csar != null) {
            newDependency.setHash(csar.getHash());
        }
        return newDependency;
    }

    public void unloadType(Topology topology, String... types) {
        ToscaTypeLoader typeLoader = initializeTypeLoader(topology);
        for (String type : types) {
            typeLoader.unloadType(type);
        }
        // FIXME if a dependency is just removed don't add it back
        for (CSARDependency updatedDependency : typeLoader.getLoadedDependencies()) {
            ToscaContext.get().updateDependency(updatedDependency);
        }
        topology.setDependencies(typeLoader.getLoadedDependencies());
    }

    /**
     * Throw an UpdateTopologyException if the topology is released
     *
     * @param topology topology to be checked
     */
    public void throwsErrorIfReleased(Topology topology) {
        if (isReleased(topology)) {
            throw new UpdateTopologyException("The topology " + topology.getId() + " cannot be updated because it's released");
        }
    }

    /**
     * True when an topology is released.
     */
    public boolean isReleased(Topology topology) {
        AbstractTopologyVersion appVersion = getApplicationVersion(topology);
        return appVersion != null && appVersion.isReleased();
    }

    /**
     * Get the delegates version of a topology.
     *
     * @param topology the topology
     * @return The application version associated with the environment.
     */
    private AbstractTopologyVersion getApplicationVersion(Topology topology) {
        if (topology.getDelegateType().equalsIgnoreCase(Application.class.getSimpleName())) {
            return applicationVersionService.getByTopologyId(topology.getId());
        } else if (topology.getDelegateType().equalsIgnoreCase(TopologyTemplate.class.getSimpleName())) {
            return topologyTemplateVersionService.getByTopologyId(topology.getId());
        }
        return null;
    }

    /**
     * Retrieve the topology template from its id
     *
     * @param topologyTemplateId the topology template's id
     * @return the required topology template
     */
    public TopologyTemplate getOrFailTopologyTemplate(String topologyTemplateId) {
        TopologyTemplate topologyTemplate = alienDAO.findById(TopologyTemplate.class, topologyTemplateId);
        if (topologyTemplate == null) {
            log.debug("Failed to recover the topology template <{}>", topologyTemplateId);
            throw new NotFoundException("Topology template with id [" + topologyTemplateId + "] cannot be found");
        }
        return topologyTemplate;
    }

    public String getYaml(Topology topology) {
        Map<String, Object> velocityCtx = new HashMap<>();
        velocityCtx.put("topology", topology);
        velocityCtx.put("template_name", "template-id");
        velocityCtx.put("template_version", "1.0.0-SNAPSHOT");
        User loggedUser = AuthorizationUtil.getCurrentUser();
        velocityCtx.put("template_author", loggedUser != null ? loggedUser.getUsername() : null);
        if (Application.class.getSimpleName().toLowerCase().equals(topology.getDelegateType())) {
            String applicationId = topology.getDelegateId();
            Application application = appService.getOrFail(applicationId);
            velocityCtx.put("template_name", application.getName());
            velocityCtx.put("application_description", application.getDescription());
            ApplicationVersion version = applicationVersionService.getByTopologyId(topology.getId());
            if (version != null) {
                velocityCtx.put("template_version", version.getVersion());
            }
        } else if (TopologyTemplate.class.getSimpleName().toLowerCase().equals(topology.getDelegateType())) {
            String topologyTemplateId = topology.getDelegateId();
            TopologyTemplate template = getOrFailTopologyTemplate(topologyTemplateId);
            velocityCtx.put("template_name", template.getName());
            velocityCtx.put("application_description", template.getDescription());
            TopologyTemplateVersion version = topologyTemplateVersionService.getByTopologyId(topology.getId());
            if (version != null) {
                velocityCtx.put("template_version", version.getVersion());
            }
        }

        try {
            StringWriter writer = new StringWriter();
            VelocityUtil.generate("templates/topology-alien_dsl_1_2_0.yml.vm", writer, velocityCtx);
            return writer.toString();
        } catch (Exception e) {
            log.error("Exception while templating YAML for topology " + topology.getId(), e);
            return ExceptionUtils.getFullStackTrace(e);
        }

    }

    public void isUniqueNodeTemplateName(Topology topology, String newNodeTemplateName) {
        if (topology.getNodeTemplates() != null && topology.getNodeTemplates().containsKey(newNodeTemplateName)) {
            log.debug("Add Node Template <{}> impossible (already exists)", newNodeTemplateName);
            // a node template already exist with the given name.
            throw new AlreadyExistException(
                    "A node template with the given name " + newNodeTemplateName + " already exists in the topology " + topology.getId() + ".");
        }
    }

    public void isUniqueRelationshipName(String topologyId, String nodeTemplateName, String newName, Set<String> relationshipNames) {
        if (relationshipNames.contains(newName)) {
            // a relation already exist with the given name.
            throw new AlreadyExistException("A relationship with the given name " + newName + " already exists in the node template " + nodeTemplateName
                    + " of topology " + topologyId + ".");
        }
    }

    public void removeNodeTemplate(String nodeTemplateName, Topology topology) {
        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
        NodeTemplate template = TopologyServiceCore.getNodeTemplate(topology.getId(), nodeTemplateName, nodeTemplates);
        template.setName(nodeTemplateName);
        removeNodeTemplate(template, topology);
    }

    public void removeNodeTemplate(NodeTemplate template, Topology topology) {
        cleanArtifactsFromRepository(template);

        // Clean up dependencies of the topology
        List<String> typesTobeUnloaded = com.google.common.collect.Lists.newArrayList();
        typesTobeUnloaded.add(template.getType());
        if (template.getRelationships() != null) {
            for (RelationshipTemplate relationshipTemplate : template.getRelationships().values()) {
                typesTobeUnloaded.add(relationshipTemplate.getType());
            }
        }
        unloadType(topology, typesTobeUnloaded.toArray(new String[typesTobeUnloaded.size()]));

        removeAndCleanTopology(template, topology);
        // update the workflows
        workflowBuilderService.removeNode(topology, template.getName(), template);
        topologyServiceCore.save(topology);
        topologyServiceCore.updateSubstitutionType(topology);
    }

    /**
     *
     * Clean a topology after removing a nodeTemplate<br>
     * relationship references, outputs, substitutions, groups
     *
     *
     * @param template
     * @param topology
     */
    public void removeAndCleanTopology(NodeTemplate template, Topology topology) {
        topology.getNodeTemplates().remove(template.getName());
        removeRelationShipReferences(template.getName(), topology);
        removeOutputs(template.getName(), topology);
        if (topology.getSubstitutionMapping() != null) {
            removeNodeTemplateSubstitutionTargetMapEntry(template.getName(), topology.getSubstitutionMapping().getCapabilities());
            removeNodeTemplateSubstitutionTargetMapEntry(template.getName(), topology.getSubstitutionMapping().getRequirements());
        }

        // group members removal
        TopologyUtils.updateGroupMembers(topology, template, template.getName(), null);
    }

    /**
     * Clean artifacts in the repository related to a node template
     *
     * @param template
     */
    public void cleanArtifactsFromRepository(NodeTemplate template) {
        // Clean up internal repository
        Map<String, DeploymentArtifact> artifacts = template.getArtifacts();
        if (artifacts != null) {
            for (Map.Entry<String, DeploymentArtifact> artifactEntry : artifacts.entrySet()) {
                DeploymentArtifact artifact = artifactEntry.getValue();
                if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
                    artifactRepository.deleteFile(artifact.getArtifactRef());
                }
            }
        }
    }

    public void removeRelationship(String nodeTemplateName, String relationshipName, Topology topology) {
        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);

        NodeTemplate template = TopologyServiceCore.getNodeTemplate(topology.getId(), nodeTemplateName, nodeTemplates);
        template.setName(nodeTemplateName);
        removeRelationship(relationshipName, template, topology);
    }

    public void removeRelationship(String relationshipName, NodeTemplate template, Topology topology) {
        log.debug("Removing the Relationship template <" + relationshipName + "> from the Node template <" + template.getName() + ">, Topology <"
                + topology.getId() + "> .");
        RelationshipTemplate relationshipTemplate = template.getRelationships().get(relationshipName);
        if (relationshipTemplate != null) {
            unloadType(topology, relationshipTemplate.getType());
            template.getRelationships().remove(relationshipName);
        } else {
            throw new NotFoundException("The relationship with name [" + relationshipName + "] do not exist for the node [" + template.getName()
                    + "] of the topology [" + topology.getId() + "]");
        }
        workflowBuilderService.removeRelationship(topology, template.getName(), relationshipName, relationshipTemplate);
        topologyServiceCore.save(topology);
    }

    private void removeNodeTemplateSubstitutionTargetMapEntry(String nodeTemplateName, Map<String, SubstitutionTarget> substitutionTargets) {
        if (substitutionTargets == null) {
            return;
        }
        Iterator<Entry<String, SubstitutionTarget>> capabilities = substitutionTargets.entrySet().iterator();
        while (capabilities.hasNext()) {
            Entry<String, SubstitutionTarget> e = capabilities.next();
            if (e.getValue().getNodeTemplateName().equals(nodeTemplateName)) {
                capabilities.remove();
            }
        }
    }

    /**
     * Remove a nodeTemplate outputs in a topology
     */
    private void removeOutputs(String nodeTemplateName, Topology topology) {
        if (topology.getOutputProperties() != null) {
            topology.getOutputProperties().remove(nodeTemplateName);
        }
        if (topology.getOutputAttributes() != null) {
            topology.getOutputAttributes().remove(nodeTemplateName);
        }
    }

    private Map<String, NodeTemplate> removeRelationShipReferences(String nodeTemplateName, Topology topology) {
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        Map<String, NodeTemplate> impactedNodeTemplates = Maps.newHashMap();
        List<String> keysToRemove = com.google.common.collect.Lists.newArrayList();
        for (String key : nodeTemplates.keySet()) {
            NodeTemplate nodeTemp = nodeTemplates.get(key);
            if (nodeTemp.getRelationships() == null) {
                continue;
            }
            keysToRemove.clear();
            for (String key2 : nodeTemp.getRelationships().keySet()) {
                RelationshipTemplate relTemp = nodeTemp.getRelationships().get(key2);
                if (relTemp == null) {
                    continue;
                }
                if (relTemp.getTarget() != null && relTemp.getTarget().equals(nodeTemplateName)) {
                    keysToRemove.add(key2);
                }
            }
            for (String relName : keysToRemove) {
                nodeTemplates.get(key).getRelationships().remove(relName);
                impactedNodeTemplates.put(key, nodeTemplates.get(key));
            }
        }
        return impactedNodeTemplates.isEmpty() ? null : impactedNodeTemplates;
    }

    public void replaceNodeTemplate(String nodeTemplateName, String newName, String newIndexedNodeType, Topology topology) {
        IndexedNodeType indexedNodeType = findIndexedNodeTypeOrFail(newIndexedNodeType);

        // Retrieve existing node template
        Map<String, NodeTemplate> nodeTemplates = TopologyServiceCore.getNodeTemplates(topology);
        NodeTemplate oldNodeTemplate = TopologyServiceCore.getNodeTemplate(topology.getId(), nodeTemplateName, nodeTemplates);
        // Load the new type to the topology in order to update its dependencies
        indexedNodeType = loadType(topology, indexedNodeType);
        // Build the new one
        NodeTemplate newNodeTemplate = buildNodeTemplate(topology.getDependencies(), indexedNodeType, null);
        newNodeTemplate.setName(newName);
        newNodeTemplate.setRelationships(oldNodeTemplate.getRelationships());
        // Put the new one in the topology
        nodeTemplates.put(newName, newNodeTemplate);

        // Unload and remove old node template
        unloadType(topology, oldNodeTemplate.getType());
        // remove the node from the workflows
        workflowBuilderService.removeNode(topology, nodeTemplateName, oldNodeTemplate);
        nodeTemplates.remove(nodeTemplateName);
        if (topology.getSubstitutionMapping() != null) {
            removeNodeTemplateSubstitutionTargetMapEntry(nodeTemplateName, topology.getSubstitutionMapping().getCapabilities());
            removeNodeTemplateSubstitutionTargetMapEntry(nodeTemplateName, topology.getSubstitutionMapping().getRequirements());
        }

        TopologyUtils.refreshNodeTempNameInRelationships(nodeTemplateName, newName, nodeTemplates);
        log.debug("Replacing the node template<{}> with <{}> bound to the node type <{}> on the topology <{}> .", nodeTemplateName, newName, newIndexedNodeType,
                topology.getId());
        // add the new node to the workflow
        workflowBuilderService.addNode(workflowBuilderService.buildTopologyContext(topology), newName, newNodeTemplate);

        topologyServiceCore.save(topology);
    }

    private IndexedNodeType findIndexedNodeTypeOrFail(final String indexedNodeTypeId) {
        IndexedNodeType indexedNodeType = alienDAO.findById(IndexedNodeType.class, indexedNodeTypeId);
        if (indexedNodeType == null) {
            throw new NotFoundException("Indexed Node Type [" + indexedNodeTypeId + "] cannot be found");
        }
        return indexedNodeType;
    }

    public void rebuildDependencies(Topology topology) {
        ToscaTypeLoader typeLoader = initializeTypeLoader(topology);
        topology.setDependencies(typeLoader.getLoadedDependencies());
    }

    private CachedFinder<Csar> buildCaheFinder() {
        return new CachedFinder<Csar>(new IFinder<Csar>() {
            @Override
            public <K extends Csar> K find(Class<K> clazz, String id) {
                return (K) csarService.findByIds(FetchContext.SUMMARY, id).get(id);
            }
        });
    }

}
