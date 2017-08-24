package alien4cloud.topology;

import static alien4cloud.utils.AlienUtils.safe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.catalog.index.ICsarDependencyLoader;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.EditionContext;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.services.EditorTopologyRecoveryHelperService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.topology.TopologyDTOBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.mapping.FilterValuesStrategy;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.exception.VersionConflictException;
import alien4cloud.model.application.Application;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.Role;
import alien4cloud.topology.exception.UpdateTopologyException;
import alien4cloud.topology.task.SuggestionsTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.tosca.container.ToscaTypeLoader;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.VersionUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TopologyService {
    @Resource
    private IToscaTypeSearchService toscaTypeSearchService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ICsarDependencyLoader csarDependencyLoader;
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private ApplicationService appService;
    @Inject
    private TopologyDTOBuilder topologyDTOBuilder;
    @Inject
    private EditorTopologyRecoveryHelperService recoveryHelperService;

    private ToscaTypeLoader initializeTypeLoader(Topology topology, boolean failOnTypeNotFound) {
        // FIXME we should use ToscaContext here, and why not allowing the caller to pass ona Context?
        ToscaTypeLoader loader = new ToscaTypeLoader(csarDependencyLoader);
        Map<String, NodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, false, failOnTypeNotFound);
        Map<String, RelationshipType> relationshipTypes = topologyServiceCore.getIndexedRelationshipTypesFromTopology(topology, failOnTypeNotFound);
        if (topology.getNodeTemplates() != null) {
            for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
                NodeType nodeType = nodeTypes.get(nodeTemplate.getType());
                // just load found types.
                // the type might be null when failOnTypeNotFound is set to false.
                if (nodeType != null) {
                    loader.loadType(nodeTemplate.getType(), csarDependencyLoader.buildDependencyBean(nodeType.getArchiveName(), nodeType.getArchiveVersion()));
                }
                if (nodeTemplate.getRelationships() != null) {

                    for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                        RelationshipType relationshipType = relationshipTypes.get(relationshipTemplate.getType());
                        // just load found types.
                        // the type might be null when failOnTypeNotFound is set to false.
                        if (relationshipType != null) {
                            loader.loadType(relationshipTemplate.getType(),
                                    csarDependencyLoader.buildDependencyBean(relationshipType.getArchiveName(), relationshipType.getArchiveVersion()));
                        }
                    }
                }
            }
        }
        if (topology.getSubstitutionMapping() != null && topology.getSubstitutionMapping().getSubstitutionType() != null) {
            NodeType substitutionType = nodeTypes.get(topology.getSubstitutionMapping().getSubstitutionType());
            loader.loadType(substitutionType.getElementId(),
                    csarDependencyLoader.buildDependencyBean(substitutionType.getArchiveName(), substitutionType.getArchiveVersion()));
            for (SubstitutionTarget substitutionTarget : safe(topology.getSubstitutionMapping().getCapabilities()).values()) {
                initializeSubstitutionTarget(loader, relationshipTypes, substitutionTarget);
            }
            for (SubstitutionTarget substitutionTarget : safe(topology.getSubstitutionMapping().getRequirements()).values()) {
                initializeSubstitutionTarget(loader, relationshipTypes, substitutionTarget);
            }
        }
        return loader;
    }

    private void initializeSubstitutionTarget(ToscaTypeLoader loader, Map<String, RelationshipType> relationshipTypes, SubstitutionTarget substitutionTarget) {
        if (substitutionTarget.getServiceRelationshipType() != null) {
            RelationshipType relationshipType = relationshipTypes.get(substitutionTarget.getServiceRelationshipType());
            if (relationshipType != null) {
                loader.loadType(substitutionTarget.getServiceRelationshipType(),
                        csarDependencyLoader.buildDependencyBean(relationshipType.getArchiveName(), relationshipType.getArchiveVersion()));
            }
        }
    }

    /**
     * Find replacements nodes for a node template
     *
     * @param nodeTemplateName the node to search for replacements
     * @param topology the topology
     * @return all possible replacement types for this node
     */
    @SneakyThrows(IOException.class)
    public NodeType[] findReplacementForNode(String nodeTemplateName, Topology topology) {
        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateName);
        Map<String, Map<String, Set<String>>> nodeTemplatesToFilters = Maps.newHashMap();
        Entry<String, NodeTemplate> nodeTempEntry = Maps.immutableEntry(nodeTemplateName, nodeTemplate);
        NodeType indexedNodeType = toscaTypeSearchService.getRequiredElementInDependencies(NodeType.class, nodeTemplate.getType(), topology.getDependencies());
        processNodeTemplate(topology, nodeTempEntry, nodeTemplatesToFilters);
        List<SuggestionsTask> topoTasks = searchForNodeTypes(topology.getWorkspace(), nodeTemplatesToFilters,
                MapUtil.newHashMap(new String[] { nodeTemplateName }, new NodeType[] { indexedNodeType }));

        if (CollectionUtils.isEmpty(topoTasks)) {
            return null;
        }
        return removeDuplicatedNodeTypeForReplacement(indexedNodeType.getElementId(), topoTasks.get(0).getSuggestedNodeTypes());
    }

    /**
     * Remove nodeType with same elementId but different version
     * and the same nodeType of with a different version
     */
    private NodeType[] removeDuplicatedNodeTypeForReplacement(String elementIdToReplace, NodeType[] suggestedNodeTypes) {
        ArrayList<NodeType> filterData = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(suggestedNodeTypes)) {
            List<String> addedArchiveName = new ArrayList<>();
            addedArchiveName.add(elementIdToReplace);

            for (int i = suggestedNodeTypes.length - 1; i >= 0; i--) {
                if (!addedArchiveName.contains(suggestedNodeTypes[i].getElementId())) {
                    addedArchiveName.add(suggestedNodeTypes[i].getElementId());
                    filterData.add(suggestedNodeTypes[i]);
                }
            }
        }
        return filterData.toArray(new NodeType[filterData.size()]);
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

    private NodeType[] getIndexedNodeTypesFromSearchResponse(final GetMultipleDataResult<NodeType> searchResult, final NodeType toExcludeIndexedNodeType)
            throws IOException {
        NodeType[] toReturnArray = null;
        for (int j = 0; j < searchResult.getData().length; j++) {
            NodeType nodeType = searchResult.getData()[j];
            if (toExcludeIndexedNodeType == null || !nodeType.getId().equals(toExcludeIndexedNodeType.getId())) {
                toReturnArray = ArrayUtils.add(toReturnArray, nodeType);
            }
        }
        return toReturnArray;
    }

    /**
     * Search for nodeTypes given some filters. Apply AND filter strategy when multiple values for a filter key.
     */
    public List<SuggestionsTask> searchForNodeTypes(String workspace, Map<String, Map<String, Set<String>>> nodeTemplatesToFilters,
            Map<String, NodeType> toExcludeIndexedNodeTypes) throws IOException {
        if (nodeTemplatesToFilters == null || nodeTemplatesToFilters.isEmpty()) {
            return null;
        }
        List<SuggestionsTask> toReturnTasks = Lists.newArrayList();
        for (Map.Entry<String, Map<String, Set<String>>> nodeTemplatesToFiltersEntry : nodeTemplatesToFilters.entrySet()) {
            Map<String, String[]> formattedFilters = Maps.newHashMap();
            Map<String, FilterValuesStrategy> filterValueStrategy = Maps.newHashMap();
            NodeType[] data = null;
            if (nodeTemplatesToFiltersEntry.getValue() != null) {
                for (Map.Entry<String, Set<String>> filterEntry : nodeTemplatesToFiltersEntry.getValue().entrySet()) {
                    formattedFilters.put(filterEntry.getKey(), filterEntry.getValue().toArray(new String[filterEntry.getValue().size()]));
                    // AND strategy if multiple values
                    filterValueStrategy.put(filterEntry.getKey(), FilterValuesStrategy.AND);
                }

                // retrieve only non abstract components
                formattedFilters.put("abstract", ArrayUtils.toArray("false"));
                // use topology workspace + global workspace
                formattedFilters.put("workspace", ArrayUtils.toArray(workspace, "ALIEN_GLOBAL_WORKSPACE"));

                GetMultipleDataResult<NodeType> searchResult = alienDAO.search(NodeType.class, null, formattedFilters, filterValueStrategy, 20);
                data = getIndexedNodeTypesFromSearchResponse(searchResult, toExcludeIndexedNodeTypes.get(nodeTemplatesToFiltersEntry.getKey()));
            }
            TaskCode taskCode = ArrayUtils.isEmpty(data) ? TaskCode.IMPLEMENT : TaskCode.REPLACE;
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
    private void checkAuthorizations(Topology topology, ApplicationRole[] applicationRoles, Role[] roles) {
        Csar relatedCsar = ToscaContext.get().getArchive(topology.getArchiveName(), topology.getArchiveVersion());
        if (Objects.equals(relatedCsar.getDelegateType(), ArchiveDelegateType.APPLICATION.toString())) {
            String applicationId = relatedCsar.getDelegateId();
            Application application = appService.getOrFail(applicationId);
            AuthorizationUtil.checkAuthorizationForApplication(application, applicationRoles);
        } else {
            AuthorizationUtil.checkHasOneRoleIn(roles);
        }
    }

    /**
     * Check that the current user can retrieve the given topology.
     *
     * @param topology The topology that is subject to being updated.
     */
    @ToscaContextual
    public void checkAccessAuthorizations(Topology topology) {
        checkAuthorizations(topology,
                new ApplicationRole[] { ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS, ApplicationRole.APPLICATION_USER },
                new Role[] { Role.COMPONENTS_BROWSER, Role.ARCHITECT });
    }

    /**
     * Check that the current user can update the given topology.
     *
     * @param topology The topology that is subject to being updated.
     */
    @ToscaContextual
    public void checkEditionAuthorizations(Topology topology) {
        checkAuthorizations(topology, new ApplicationRole[] { ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS },
                new Role[] { Role.ARCHITECT });
    }

    /**
     * Load a type into the topology (add dependency for this new type, upgrade if necessary ...)
     *
     * If the dependency added has been upgraded into the topology, then recover the topology
     *
     * @param topology the topology
     * @param element the element to load
     * @param <T> tosca element type
     * @return The real loaded element if element given in argument is from older archive than topology's dependencies
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractToscaType> T loadType(Topology topology, T element) {
        String type = element.getElementId();
        String archiveName = element.getArchiveName();
        String archiveVersion = element.getArchiveVersion();
        CSARDependency toLoadDependency = csarDependencyLoader.buildDependencyBean(archiveName, archiveVersion);

        // FIXME Transitive dependencies could change here and thus types be affected ?
        ToscaTypeLoader typeLoader = initializeTypeLoader(topology, true);
        boolean upgraded = typeLoader.loadType(type, toLoadDependency);
        ToscaContext.get().resetDependencies(typeLoader.getLoadedDependencies());
        // Validate does not induce missing types
        try {
            this.checkForMissingTypes(topology);
        } catch (NotFoundException e) {
            // Revert changes made to the Context then throw.
            ToscaContext.get().resetDependencies(topology.getDependencies());
            throw new VersionConflictException("Adding the type [" + element.getId() + "] from archive [" + element.getArchiveName() + ":"
                    + element.getArchiveVersion() + "] changes the topology dependencies and induces missing types. "
                    + "Try with another version instead. Not found : [" + e.getMessage() + "].", e);
        }

        topology.setDependencies(typeLoader.getLoadedDependencies());

        // recover the topology if needed
        if (upgraded) {
            recover(topology, toLoadDependency);
        }

        return ToscaContext.getOrFail((Class<T>) element.getClass(), type);
    }

    public void unloadType(Topology topology, String... types) {
        // make sure to set the failOnTypeNotFound to false, to deal with topology recovering when a type is deleted from a dependency
        ToscaTypeLoader typeLoader = initializeTypeLoader(topology, false);
        for (String type : types) {
            typeLoader.unloadType(type);
        }
        ToscaContext.get().resetDependencies(typeLoader.getLoadedDependencies());
        // TODO update csar dependencies also ?
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
        return !VersionUtil.isSnapshot(topology.getArchiveVersion());
    }

    public void isUniqueNodeTemplateName(Topology topology, String newNodeTemplateName) {
        if (topology.getNodeTemplates() != null && topology.getNodeTemplates().containsKey(newNodeTemplateName)) {
            log.debug("Add Node Template <{}> impossible (already exists)", newNodeTemplateName);
            // a node template already exist with the given name.
            throw new AlreadyExistException(
                    "A node template with the given name " + newNodeTemplateName + " already exists in the topology " + topology.getId() + ".");
        }
    }

    public void updateDependencies(EditionContext context, CSARDependency newDependency) {
        final Set<CSARDependency> oldDependencies = new HashSet<>(context.getTopology().getDependencies());
        final Set<CSARDependency> newDependencies = csarDependencyLoader.getDependencies(newDependency.getName(), newDependency.getVersion());
        newDependencies.add(newDependency);

        // Update context with the new dependencies.
        newDependencies.forEach(csarDependency -> context.getToscaContext().updateDependency(csarDependency));

        // Validate that the dependency change does not induce missing types.
        try {
            this.checkForMissingTypes(context.getTopology());
        } catch (NotFoundException e) {
            // Revert changes made to the Context then throw.
            context.getToscaContext().resetDependencies(oldDependencies);
            context.getTopology().setDependencies(oldDependencies);
            throw new VersionConflictException("Changing the dependency [" + newDependency.getName() + "] to version [" + newDependency.getVersion()
                    + "] induces missing types in the topology. Not found : [" + e.getMessage() + "].", e);
        }

        // Perform the dependency update on the topology.
        context.getTopology().setDependencies(new HashSet<>(context.getToscaContext().getDependencies()));
    }

    /**
     * Check for missing types in the Topology
     *
     * @param topology the topology
     * @throws NotFoundException if the Type is used in the topology and not found in its context.
     */
    private void checkForMissingTypes(Topology topology) {
        /*
         * TODO: Cache the result or do not use TopologyDTOBuilder
         * because it is then called again at the end of the operation execution (EditorController.execute)
         */
        TopologyDTO topologyDTO = topologyDTOBuilder.initTopologyDTO(topology, new TopologyDTO());
        topologyDTO.getNodeTypes().forEach(throwTypeNotFound());
        topologyDTO.getRelationshipTypes().forEach(throwTypeNotFound());
        topologyDTO.getCapabilityTypes().forEach(throwTypeNotFound());
        topologyDTO.getDataTypes().forEach(throwTypeNotFound());
    }

    private BiConsumer<String, AbstractToscaType> throwTypeNotFound() {
        return (s, type) -> {
            if (type == null) {
                throw new NotFoundException(s);
            }
        };
    }

    public void rebuildDependencies(Topology topology) {
        ToscaTypeLoader typeLoader = initializeTypeLoader(topology, true);
        topology.setDependencies(typeLoader.getLoadedDependencies());
    }

    /**
     * Recover a topology, given a set of dependencies (that might have changed)
     * 
     * @param topology The topology to recover
     * @param dependencies The updated dependencies within the topology for witch to process the recovery
     */
    public void recover(Topology topology, CSARDependency... dependencies) {
        // get recovering operations
        List<AbstractEditorOperation> recoveringOperations = recoveryHelperService.buildRecoveryOperations(topology, Sets.newHashSet(dependencies));
        // process every recovery operation
        recoveryHelperService.processRecoveryOperations(topology, recoveringOperations);
    };

}
