package alien4cloud.rest.topology;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.mapping.FilterValuesStrategy;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.exception.VersionConflictException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.RequirementDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Requirement;
import alien4cloud.model.topology.Topology;
import alien4cloud.rest.topology.task.PropertiesTask;
import alien4cloud.rest.topology.task.RequirementToSatify;
import alien4cloud.rest.topology.task.RequirementsTask;
import alien4cloud.rest.topology.task.SuggestionsTask;
import alien4cloud.rest.topology.task.TaskCode;
import alien4cloud.rest.topology.task.TopologyTask;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.Role;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.container.ToscaTypeLoader;
import alien4cloud.tosca.serializer.VelocityUtil;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.VersionUtil;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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
    private ApplicationEnvironmentService applicationEnvironmentService;

    private ToscaTypeLoader initializeTypeLoader(Topology topology) {
        ToscaTypeLoader loader = new ToscaTypeLoader(csarService);
        Map<String, IndexedNodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, false);
        Map<String, IndexedRelationshipType> relationshipTypes = topologyServiceCore.getIndexedRelationshipTypesFromTopology(topology);
        if (topology.getNodeTemplates() != null) {
            for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
                IndexedNodeType nodeType = nodeTypes.get(nodeTemplate.getType());
                loader.loadType(nodeTemplate.getType(), new CSARDependency(nodeType.getArchiveName(), nodeType.getArchiveVersion()));
                if (nodeTemplate.getRelationships() != null) {
                    for (RelationshipTemplate relationshipTemplate : nodeTemplate.getRelationships().values()) {
                        IndexedRelationshipType relationshipType = relationshipTypes.get(relationshipTemplate.getType());
                        loader.loadType(relationshipTemplate.getType(),
                                new CSARDependency(relationshipType.getArchiveName(), relationshipType.getArchiveVersion()));
                    }
                }
            }
        }
        return loader;
    }

    /**
     * Get the relationships from a topology
     *
     * @param topo
     * @param abstractOnes if only abstract ones should be retrieved
     * @return
     */
    private Map<String, IndexedRelationshipType[]> getIndexedRelationshipTypesFromTopology(Topology topo, Boolean abstractOnes) {
        Map<String, IndexedRelationshipType[]> indexedRelationshipTypesMap = Maps.newHashMap();
        if (topo.getNodeTemplates() == null) {
            return indexedRelationshipTypesMap;
        }
        for (Map.Entry<String, NodeTemplate> template : topo.getNodeTemplates().entrySet()) {
            if (template.getValue().getRelationships() == null) {
                continue;
            }

            Set<IndexedRelationshipType> indexedRelationshipTypes = Sets.newHashSet();
            for (RelationshipTemplate relTemplate : template.getValue().getRelationships().values()) {
                IndexedRelationshipType indexedRelationshipType = csarRepoSearchService.getElementInDependencies(IndexedRelationshipType.class,
                        relTemplate.getType(), topo.getDependencies());
                if (indexedRelationshipType != null) {
                    if (abstractOnes == null || abstractOnes.equals(indexedRelationshipType.isAbstract())) {
                        indexedRelationshipTypes.add(indexedRelationshipType);
                    }
                } else {
                    throw new NotFoundException("Relationship Type [" + relTemplate.getType() + "] cannot be found");
                }
            }
            if (indexedRelationshipTypes.size() > 0) {
                indexedRelationshipTypesMap.put(template.getKey(), indexedRelationshipTypes.toArray(new IndexedRelationshipType[0]));
            }

        }
        return indexedRelationshipTypesMap;
    }

    /**
     *
     * Get all the relationships in which a given nodetemplate is a target
     *
     * @param nodeTemplateName
     * @param nodeTemplates
     * @return
     */
    private List<RelationshipTemplate> getTargetRelatedRelatonshipsTemplate(String nodeTemplateName, Map<String, NodeTemplate> nodeTemplates) {
        List<RelationshipTemplate> toReturn = Lists.newArrayList();
        for (String key : nodeTemplates.keySet()) {
            NodeTemplate nodeTemp = nodeTemplates.get(key);
            if (nodeTemp.getRelationships() == null) {
                continue;
            }
            for (String key2 : nodeTemp.getRelationships().keySet()) {
                RelationshipTemplate relTemp = nodeTemp.getRelationships().get(key2);
                if (relTemp == null) {
                    continue;
                }
                if (relTemp.getTarget() != null && relTemp.getTarget().equals(nodeTemplateName)) {
                    toReturn.add(relTemp);
                }
            }
        }

        return toReturn;
    }

    /**
     *
     * constructs a TopologyTask list given a Map (nodetemplatename => component) and the code
     *
     * @param components
     * @param taskCode
     * @return
     */
    private <T extends IndexedInheritableToscaElement> List<TopologyTask> getTaskListFromMapArray(Map<String, T[]> components, TaskCode taskCode) {
        List<TopologyTask> taskList = Lists.newArrayList();
        for (Entry<String, T[]> entry : components.entrySet()) {
            for (IndexedInheritableToscaElement compo : entry.getValue()) {
                TopologyTask task = new TopologyTask();
                task.setNodeTemplateName(entry.getKey());
                task.setComponent(compo);
                task.setCode(taskCode);
                taskList.add(task);
            }
        }
        if (taskList.isEmpty()) {
            return null;
        } else {
            return taskList;
        }
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
     * Find replacements components for abstract nodes in a Topology
     *
     * @param topology
     * @return
     */
    @SneakyThrows({ IOException.class })
    private List<SuggestionsTask> findReplacementForAbstracts(Topology topology) {
        Map<String, IndexedNodeType> nodeTempNameToAbstractIndexedNodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, true, true);
        Map<String, Map<String, Set<String>>> nodeTemplatesToFilters = Maps.newHashMap();
        for (Entry<String, IndexedNodeType> idntEntry : nodeTempNameToAbstractIndexedNodeTypes.entrySet()) {
            processNodeTemplate(topology, Maps.immutableEntry(idntEntry.getKey(), topology.getNodeTemplates().get(idntEntry.getKey())), nodeTemplatesToFilters);
        }
        // processAbstractNodeTemplate(topology, nodeTempNameToAbstractIndexedNodeTypes, nodeTempEntry, nodeTemplatesToFilters);
        // relatedIndexedNodeTypes.put(nodeTempEntry.getKey(), nodeTempNameToAbstractIndexedNodeTypes.get(nodeTempEntry.getValue()));
        return searchForNodeTypes(nodeTemplatesToFilters, nodeTempNameToAbstractIndexedNodeTypes);
    }

    /**
     *
     * find replacements nodes for a node template
     *
     * @param nodeTemplateName
     * @param topology
     * @return
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

    /**
     *
     * Validate if a topology is deployable or not
     *
     * @param topology
     * @return
     */
    public ValidTopologyDTO validateTopology(Topology topology, Map<String, String> inputs) {
        ValidTopologyDTO dto = new ValidTopologyDTO();
        if (topology.getNodeTemplates() == null || topology.getNodeTemplates().size() < 1) {
            dto.setValid(false);
            return dto;
        }

        // validate abstract relationships
        dto.addToTaskList(validateAbstractRelationships(topology));

        // validate abstract node types and find suggestions
        dto.addToTaskList(findReplacementForAbstracts(topology));

        // validate requirements lowerBounds
        dto.addToTaskList(validateRequirementsLowerBounds(topology));

        // validate required properties (properties of NodeTemplate, Relationship and Capability)
        dto.addToTaskList(validateProperties(topology, inputs));

        dto.setValid(CollectionUtils.isEmpty(dto.getTaskList()));

        return dto;
    }

    private List<TopologyTask> validateAbstractRelationships(Topology topology) {
        Map<String, IndexedRelationshipType[]> abstractIndexedRelationshipTypes = getIndexedRelationshipTypesFromTopology(topology, true);
        return getTaskListFromMapArray(abstractIndexedRelationshipTypes, TaskCode.IMPLEMENT);
    }

    /**
     * check if the upperBound of a requirement is reached on a node template
     *
     *
     * @param nodeTemplate
     * @param requirementName
     * @param dependencies
     * @return
     */
    public boolean isRequirementUpperBoundReachedForSource(NodeTemplate nodeTemplate, String requirementName, Set<CSARDependency> dependencies) {
        IndexedNodeType relatedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                dependencies);
        Requirement requirement = nodeTemplate.getRequirements().get(requirementName);
        if (nodeTemplate.getRelationships() == null || nodeTemplate.getRelationships().isEmpty()) {
            return false;
        }

        RequirementDefinition requirementDefinition = getRequirementDefinition(relatedIndexedNodeType.getRequirements(), requirementName, requirement.getType());

        if (requirementDefinition.getUpperBound() == Integer.MAX_VALUE) {
            return false;
        }

        int count = countRelationshipsForRequirement(requirementName, requirement.getType(), nodeTemplate.getRelationships());

        return count >= requirementDefinition.getUpperBound();
    }

    public boolean isCapabilityUpperBoundReachedForTarget(String nodeTemplateName, Map<String, NodeTemplate> nodeTemplates, String capabilityName,
            Set<CSARDependency> dependencies) {
        NodeTemplate nodeTemplate = nodeTemplates.get(nodeTemplateName);
        IndexedNodeType relatedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                dependencies);
        chekCapability(nodeTemplateName, capabilityName, nodeTemplate);

        CapabilityDefinition capabilityDefinition = getCapabilityDefinition(relatedIndexedNodeType.getCapabilities(), capabilityName);
        if (capabilityDefinition.getUpperBound() == Integer.MAX_VALUE) {
            return false;
        }

        List<RelationshipTemplate> targetRelatedRelationships = getTargetRelatedRelatonshipsTemplate(nodeTemplateName, nodeTemplates);
        if (targetRelatedRelationships == null || targetRelatedRelationships.isEmpty()) {
            return false;
        }

        int count = 0;
        for (RelationshipTemplate rel : targetRelatedRelationships) {
            if (rel.getTargetedCapabilityName().equals(capabilityName)) {
                count++;
            }
        }

        return count >= capabilityDefinition.getUpperBound();
    }

    private void chekCapability(String nodeTemplateName, String capabilityName, NodeTemplate nodeTemplate) {
        boolean capablityExists = false;
        if (nodeTemplate.getCapabilities() != null) {
            for (Entry<String, Capability> capaEntry : nodeTemplate.getCapabilities().entrySet()) {
                if (capaEntry.getKey().equals(capabilityName)) {
                    capablityExists = true;
                }
            }
        }
        if (!capablityExists) {
            throw new NotFoundException("A capability with name [" + capabilityName + "] cannot be found in the target node [" + nodeTemplateName + "].");
        }
    }

    private RequirementDefinition getRequirementDefinition(Collection<RequirementDefinition> requirementDefinitions, String requirementName,
            String requirementType) {

        for (RequirementDefinition requirementDef : requirementDefinitions) {
            if (requirementDef.getId().equals(requirementName) && requirementDef.getType().equals(requirementType)) {
                return requirementDef;
            }
        }

        throw new NotFoundException("Requirement definition [" + requirementName + ":" + requirementType + "] cannot be found");
    }

    private CapabilityDefinition getCapabilityDefinition(Collection<CapabilityDefinition> capabilityDefinitions, String capabilityName) {

        for (CapabilityDefinition capabilityDef : capabilityDefinitions) {
            if (capabilityDef.getId().equals(capabilityName)) {
                return capabilityDef;
            }
        }

        throw new NotFoundException("Capability definition [" + capabilityName + "] cannot be found");
    }

    /**
     *
     * @param nodeTempName
     * @param filterKey
     * @param filterValueToAdd
     * @param nodeTemplatesToFilters
     */
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
     * Process a node template to retrieve filters for node replacements search
     *
     * @param topology
     * @param nodeTempEntry
     * @param nodeTemplatesToFilters
     */
    private void processNodeTemplate(final Topology topology, final Entry<String, NodeTemplate> nodeTempEntry,
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
        List<RelationshipTemplate> relTemplatesTargetRelated = getTargetRelatedRelatonshipsTemplate(nodeTempEntry.getKey(), topology.getNodeTemplates());
        for (RelationshipTemplate relationshipTemplate : relTemplatesTargetRelated) {
            addFilters(nodeTempEntry.getKey(), capabilityFilterKey, relationshipTemplate.getRequirementType(), nodeTemplatesToFilters);
        }

    }

    private IndexedNodeType[] getIndexedNodeTypesFromSearchResponse(final GetMultipleDataResult searchResult, final IndexedNodeType toExcludeIndexedNodeType)
            throws IOException {
        IndexedNodeType[] toReturnArray = null;
        for (int j = 0; j < searchResult.getData().length; j++) {
            IndexedNodeType nodeType = JsonUtil.readObject(JsonUtil.toString(searchResult.getData()[j]), IndexedNodeType.class);
            if (toExcludeIndexedNodeType == null || !nodeType.getId().equals(toExcludeIndexedNodeType.getId())) {
                toReturnArray = ArrayUtils.add(toReturnArray, nodeType);
            }
        }
        return toReturnArray;
    }

    /**
     * search for nodeTypes given some filters.
     * Apply AND filter strategy when multiple values for a filter key.
     *
     * @param nodeTemplatesToFilters
     * @param toExcludeIndexedNodeTypes
     *            : the indexed types to exclude from the results
     * @return
     * @throws IOException
     */
    private List<SuggestionsTask> searchForNodeTypes(Map<String, Map<String, Set<String>>> nodeTemplatesToFilters,
            Map<String, IndexedNodeType> toExcludeIndexedNodeTypes) throws IOException {
        if (nodeTemplatesToFilters == null || nodeTemplatesToFilters.isEmpty()) {
            return null;
        }
        List<SuggestionsTask> toReturnTasks = Lists.newArrayList();
        for (Map.Entry<String, Map<String, Set<String>>> nodeTemplatesToFiltersEntry : nodeTemplatesToFilters.entrySet()) {
            Map<String, String[]> formatedFilters = Maps.newHashMap();
            Map<String, FilterValuesStrategy> filterValueStrategy = Maps.newHashMap();
            IndexedNodeType[] data = null;
            if (nodeTemplatesToFiltersEntry.getValue() != null) {
                for (Map.Entry<String, Set<String>> filterEntry : nodeTemplatesToFiltersEntry.getValue().entrySet()) {
                    formatedFilters.put(filterEntry.getKey(), filterEntry.getValue().toArray(new String[0]));
                    // AND strategy if multiple values
                    filterValueStrategy.put(filterEntry.getKey(), FilterValuesStrategy.AND);
                }

                // retrieve only non abstract components
                formatedFilters.put("abstract", ArrayUtils.toArray("false"));

                GetMultipleDataResult searchResult = alienDAO.search(IndexedNodeType.class, null, formatedFilters, filterValueStrategy, 20);
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

    private List<RequirementsTask> validateRequirementsLowerBounds(Topology topology) {
        List<RequirementsTask> toReturnTaskList = Lists.newArrayList();
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        for (Entry<String, NodeTemplate> nodeTempEntry : nodeTemplates.entrySet()) {
            NodeTemplate nodeTemp = nodeTempEntry.getValue();
            if (nodeTemp.getRequirements() == null) {
                continue;
            }
            IndexedNodeType relatedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemp.getType(),
                    topology.getDependencies());
            // do pass if abstract node
            if (relatedIndexedNodeType.isAbstract()) {
                continue;
            }
            RequirementsTask task = new RequirementsTask();
            task.setNodeTemplateName(nodeTempEntry.getKey());
            task.setCode(TaskCode.SATISFY_LOWER_BOUND);
            task.setComponent(relatedIndexedNodeType);
            task.setRequirementsToImplement(Lists.<RequirementToSatify> newArrayList());
            for (RequirementDefinition reqDef : relatedIndexedNodeType.getRequirements()) {
                int count = countRelationshipsForRequirement(reqDef.getId(), reqDef.getType(), nodeTemp.getRelationships());
                if (count < reqDef.getLowerBound()) {
                    task.getRequirementsToImplement().add(new RequirementToSatify(reqDef.getId(), reqDef.getType(), reqDef.getLowerBound() - count));
                }
            }
            if (CollectionUtils.isNotEmpty(task.getRequirementsToImplement())) {
                toReturnTaskList.add(task);
            }
        }
        return toReturnTaskList.isEmpty() ? null : toReturnTaskList;
    }

    private List<PropertiesTask> validateProperties(Topology topology, Map<String, String> inputs) {
        List<PropertiesTask> toReturnTaskList = Lists.newArrayList();
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        for (Entry<String, NodeTemplate> nodeTempEntry : nodeTemplates.entrySet()) {
            NodeTemplate nodeTemplate = nodeTempEntry.getValue();
            if (nodeTemplate.getProperties() == null || nodeTemplate.getProperties().isEmpty()) {
                continue;
            }
            IndexedNodeType relatedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                    topology.getDependencies());
            // do pass if abstract node
            if (relatedIndexedNodeType.isAbstract()) {
                continue;
            }
            PropertiesTask task = new PropertiesTask();
            task.setNodeTemplateName(nodeTempEntry.getKey());
            task.setCode(TaskCode.PROPERTY_REQUIRED);
            task.setComponent(relatedIndexedNodeType);
            task.setProperties(Lists.<String> newArrayList());

            // Check the properties of node template
            addRequiredPropertyIdToTaskProperties(nodeTemplate.getProperties(), relatedIndexedNodeType.getProperties(), inputs, task);

            // Check relationships PD
            if (nodeTemplate.getRelationships() != null && !nodeTemplate.getRelationships().isEmpty()) {
                Collection<RelationshipTemplate> relationships = nodeTemplate.getRelationships().values();
                for (RelationshipTemplate relationship : relationships) {
                    if (relationship.getProperties() == null || relationship.getProperties().isEmpty()) {
                        continue;
                    }
                    addRequiredPropertyIdToTaskProperties(relationship.getProperties(), getRelationshipPropertyDefinition(topology, nodeTemplate), inputs, task);
                }
            }

            // Check capabilities PD
            if (nodeTemplate.getCapabilities() != null && !nodeTemplate.getCapabilities().isEmpty()) {
                Collection<Capability> capabilities = nodeTemplate.getCapabilities().values();
                for (Capability capability : capabilities) {
                    if (capability.getProperties() == null || capability.getProperties().isEmpty()) {
                        continue;
                    }
                    addRequiredPropertyIdToTaskProperties(capability.getProperties(), getCapabilitiesPropertyDefinition(topology, nodeTemplate), inputs, task);
                }
            }

            if (CollectionUtils.isNotEmpty(task.getProperties())) {
                toReturnTaskList.add(task);
            }
        }
        return toReturnTaskList.isEmpty() ? null : toReturnTaskList;
    }

    private void addRequiredPropertyIdToTaskProperties(Map<String, AbstractPropertyValue> properties, Map<String, PropertyDefinition> relatedProperties,
            Map<String, String> inputs, PropertiesTask task) {
        for (Entry<String, AbstractPropertyValue> propertyEntry : properties.entrySet()) {
            PropertyDefinition propertyDef = relatedProperties.get(propertyEntry.getKey());
            // check value
            AbstractPropertyValue value = propertyEntry.getValue();
            String propertyValue = null;
            if (value instanceof ScalarPropertyValue) {
                propertyValue = ((ScalarPropertyValue) value).getValue();
            } else if (value instanceof FunctionPropertyValue && inputs != null) {
                propertyValue = inputs.get(((FunctionPropertyValue) value).getTemplateName());
            }
            if (propertyDef.isRequired() && StringUtils.isBlank(propertyValue)) {
                task.getProperties().add(propertyEntry.getKey());
            }
        }
    }

    private Map<String, PropertyDefinition> getCapabilitiesPropertyDefinition(Topology topology, NodeTemplate nodeTemplate) {
        Map<String, PropertyDefinition> relatedProperties = Maps.newTreeMap();

        for (Entry<String, Capability> capabilityEntry : nodeTemplate.getCapabilities().entrySet()) {
            IndexedCapabilityType indexedCapabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class, capabilityEntry
                    .getValue().getType(), topology.getDependencies());
            if (indexedCapabilityType.getProperties() != null && !indexedCapabilityType.getProperties().isEmpty()) {
                relatedProperties.putAll(indexedCapabilityType.getProperties());
            }
        }

        return relatedProperties;
    }

    private Map<String, PropertyDefinition> getRelationshipPropertyDefinition(Topology topology, NodeTemplate nodeTemplate) {
        Map<String, PropertyDefinition> relatedProperties = Maps.newTreeMap();

        for (Entry<String, RelationshipTemplate> relationshipTemplateEntry : nodeTemplate.getRelationships().entrySet()) {
            IndexedRelationshipType indexedRelationshipType = csarRepoSearchService.getRequiredElementInDependencies(IndexedRelationshipType.class,
                    relationshipTemplateEntry.getValue().getType(), topology.getDependencies());
            if (indexedRelationshipType.getProperties() != null && !indexedRelationshipType.getProperties().isEmpty()) {
                relatedProperties.putAll(indexedRelationshipType.getProperties());
            }
        }

        return relatedProperties;
    }

    private int countRelationshipsForRequirement(String requirementName, String requirementType, Map<String, RelationshipTemplate> relationships) {
        int count = 0;
        if (relationships == null) {
            return 0;
        }
        for (Entry<String, RelationshipTemplate> relEntry : relationships.entrySet()) {
            if (relEntry.getValue().getRequirementName().equals(requirementName) && relEntry.getValue().getRequirementType().equals(requirementType)) {
                count++;
            }
        }
        return count;
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

    private String toLowerCase(String text) {
        return text.substring(0, 1).toLowerCase() + text.substring(1);
    }

    private String toUpperCase(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /**
     * Construct a relationship name from target and relationship type.
     *
     * @param type
     * @param targetName
     * @return
     */
    public String getRelationShipName(String type, String targetName) {
        String[] tokens = type.split("\\.");
        if (tokens.length > 1) {
            return toLowerCase(tokens[tokens.length - 1]) + toUpperCase(targetName);
        } else {
            return toLowerCase(type) + toUpperCase(targetName);
        }
    }

    /**
     * Create a {@link TopologyDTO} from a topology by fetching node types, relationship types and capability types used in the topology.
     *
     * @param topology The topology for which to create a DTO.
     * @return The {@link TopologyDTO} that contains the given topology
     */
    public TopologyDTO buildTopologyDTO(Topology topology) {
        Map<String, IndexedNodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, false);
        Map<String, IndexedRelationshipType> relationshipTypes = topologyServiceCore.getIndexedRelationshipTypesFromTopology(topology);
        Map<String, IndexedCapabilityType> capabilityTypes = getIndexedCapabilityTypes(nodeTypes.values(), topology.getDependencies());
        String yaml = getYaml(topology);
        Map<String, Map<String, Set<String>>> outputCapabilityProperties = topology.getOutputCapabilityProperties();
        return new TopologyDTO(topology, nodeTypes, relationshipTypes, capabilityTypes, outputCapabilityProperties, yaml);
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
     * @param <T>
     * @return The real loaded element if element given in argument is from older archive than topology's dependencies
     */
    public <T extends IndexedToscaElement> T loadType(Topology topology, T element) {
        String type = element.getElementId();
        String archiveName = element.getArchiveName();
        String archiveVersion = element.getArchiveVersion();
        CSARDependency newDependency = new CSARDependency(archiveName, archiveVersion);
        CSARDependency topologyDependency = getDependencyWithName(topology, archiveName);
        if (topologyDependency != null) {
            int comparisonResult = VersionUtil.compare(newDependency.getVersion(), topologyDependency.getVersion());
            if (comparisonResult > 0) {
                // Dependency of the type is more recent, try to upgrade the topology
                topology.getDependencies().add(newDependency);
                topology.getDependencies().remove(topologyDependency);
                Map<String, IndexedNodeType> nodeTypes = null;
                Map<String, IndexedRelationshipType> relationshipTypes = null;
                try {
                    nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, false);
                    relationshipTypes = topologyServiceCore.getIndexedRelationshipTypesFromTopology(topology);
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
                        newNodeTemplates.put(nodeTemplateEntry.getKey(), newNodeTemplate);
                    }
                    topology.setNodeTemplates(newNodeTemplates);
                }
            } else if (comparisonResult < 0) {
                // Dependency of the topology is more recent, try to upgrade the dependency of the type
                element = (T) csarRepoSearchService.getElementInDependencies(element.getClass(), element.getElementId(), topology.getDependencies());
            }
        }
        ToscaTypeLoader typeLoader = initializeTypeLoader(topology);
        typeLoader.loadType(type, new CSARDependency(element.getArchiveName(), element.getArchiveVersion()));
        topology.setDependencies(typeLoader.getLoadedDependencies());
        return element;
    }

    public void unloadType(Topology topology, String... types) {
        ToscaTypeLoader typeLoader = initializeTypeLoader(topology);
        for (String type : types) {
            typeLoader.unloadType(type);
        }
        topology.setDependencies(typeLoader.getLoadedDependencies());
    }

    /**
     * Throw an UpdateTopologyException if the topology is released
     *
     * @param topology
     */
    public void throwsErrorIfReleased(Topology topology) {
        if (isReleased(topology)) {
            throw new UpdateTopologyException("The topology " + topology.getId() + " cannot be updated because it's released");
        }
    }

    /**
     * True when an topology is released
     *
     * @param topology
     * @return true if the environment is currently deployed
     */
    public boolean isReleased(Topology topology) {
        ApplicationVersion appVersion = getApplicationVersion(topology);
        if (appVersion == null || !appVersion.isReleased()) {
            return false;
        }
        return true;
    }

    /**
     * Get the released application version of a topology
     *
     * @param applicationVersionId The id of the application version of a topology
     * @return The application version associated with the environment.
     */
    private ApplicationVersion getApplicationVersion(Topology topology) {
        GetMultipleDataResult<ApplicationVersion> dataResult = alienDAO.search(
                ApplicationVersion.class,
                null,
                MapUtil.newHashMap(new String[] { "applicationId", "topologyId" }, new String[][] { new String[] { topology.getDelegateId() },
                        new String[] { topology.getId() } }), 1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            return dataResult.getData()[0];
        }
        return null;
    }

    /**
     * Retrieve the topology template from its id
     *
     * @param topologyTemplateId
     * @return
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
        Map<String, Object> velocityCtx = new HashMap<String, Object>();
        velocityCtx.put("topology", topology);
        velocityCtx.put("template_name", "template-id");
        velocityCtx.put("template_version", "1.0.0-SNAPSHOT");
        velocityCtx.put("template_author", AuthorizationUtil.getCurrentUser().getUsername());

        if (topology.getDelegateType().equals(Application.class.getSimpleName().toLowerCase())) {
            String applicationId = topology.getDelegateId();
            Application application = appService.getOrFail(applicationId);
            velocityCtx.put("template_name", application.getName());
            velocityCtx.put("application_description", application.getDescription());
            ApplicationVersion version = applicationVersionService.getByTopologyId(topology.getId());
            if (version != null) {
                velocityCtx.put("template_version", version.getVersion());
            }
        } else if (topology.getDelegateType().equals(TopologyTemplate.class.getSimpleName().toLowerCase())) {
            String topologyTemplateId = topology.getDelegateId();
            TopologyTemplate template = getOrFailTopologyTemplate(topologyTemplateId);
            velocityCtx.put("template_name", template.getName());
            velocityCtx.put("application_description", template.getDescription());
        }

        try {
            StringWriter writer = new StringWriter();
            VelocityUtil.generate("templates/topology-1_0_0_wd03.yml.vm", writer, velocityCtx);
            return writer.toString();
        } catch (Exception e) {
            log.error("Exception while templating YAML for topology " + topology.getId(), e);
            return ExceptionUtils.getFullStackTrace(e);
        }

    }

}
