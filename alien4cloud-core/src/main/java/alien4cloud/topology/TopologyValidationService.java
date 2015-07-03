package alien4cloud.topology;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.cloud.CloudService;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.common.TagService;
import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.AvailabilityZone;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.common.InternalMetaProperties;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.common.MetaPropertiesTarget;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.model.components.FilterDefinition;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.RequirementDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Requirement;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.exception.AvailabilityZoneConfigurationException;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.paas.ha.AllocationError;
import alien4cloud.paas.ha.AllocationErrorCode;
import alien4cloud.paas.ha.AvailabilityZoneAllocator;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.paas.plan.TopologyTreeBuilderService;
import alien4cloud.topology.task.HAGroupTask;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.RequirementToSatify;
import alien4cloud.topology.task.RequirementsTask;
import alien4cloud.topology.task.ScalableTask;
import alien4cloud.topology.task.SuggestionsTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import alien4cloud.topology.task.TopologyTask;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.NormativeComputeConstants;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Service
@Slf4j
public class TopologyValidationService {

    @Resource
    private CSARRepositorySearchService csarRepoSearchService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private TopologyService topologyService;

    @Resource
    private TopologyTreeBuilderService topologyTreeBuilderService;

    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    @Resource
    private CloudService cloudService;

    @Resource
    private MetaPropertiesService metaPropertiesService;

    @Resource
    private ApplicationService applicationService;

    @Resource
    private TagService tagService;

    @Resource
    private ConstraintPropertyService constraintPropertyService;

    private List<RequirementsTask> validateRequirementsLowerBounds(Topology topology) {
        List<RequirementsTask> toReturnTaskList = Lists.newArrayList();
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTempEntry : nodeTemplates.entrySet()) {
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
            if (CollectionUtils.isNotEmpty(relatedIndexedNodeType.getRequirements())) {
                for (RequirementDefinition reqDef : relatedIndexedNodeType.getRequirements()) {
                    int count = countRelationshipsForRequirement(reqDef.getId(), reqDef.getType(), nodeTemp.getRelationships());
                    if (count < reqDef.getLowerBound()) {
                        task.getRequirementsToImplement().add(new RequirementToSatify(reqDef.getId(), reqDef.getType(), reqDef.getLowerBound() - count));
                        continue;
                    }

                    // now, we test node filters
                    if (reqDef.getNodeFilter() == null) {
                        continue;
                    }
                    if (reqDef.getNodeFilter().getProperties() != null && !reqDef.getNodeFilter().getProperties().isEmpty()) {
                        Map<String, List<PropertyConstraint>> properties = reqDef.getNodeFilter().getProperties();
                        for (Entry<String, List<PropertyConstraint>> propertyEntry : properties.entrySet()) {
                            for (PropertyConstraint constraint : propertyEntry.getValue()) {
                                NodeTemplate source = topology.getNodeTemplates().get(nodeTempEntry.getKey());
                                // TODO : get the relationship name
                                String targetName = source.getRelationships().get("hostedOnCompute").getTarget();
                                NodeTemplate target = topology.getNodeTemplates().get(targetName);
                                AbstractPropertyValue propertyValue = target.getProperties().get(propertyEntry.getKey());

                                // Get target nodetype to fond the PD
                                Map<String, IndexedNodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true);
                                IndexedNodeType targetIndexedNodeType = nodeTypes.get(targetName);
                                IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(targetIndexedNodeType.getProperties().get(propertyEntry.getKey())
                                        .getType());

                                try {
                                    constraint.initialize(toscaType);
                                    // TODO : also check the FunctionPropertyValue
                                    constraint.validate(toscaType, FunctionEvaluator.getScalarValue(propertyValue));
                                } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
                                    task.getRequirementsToImplement().add(
                                            new RequirementToSatify(reqDef.getId(), propertyEntry.getKey(), reqDef.getLowerBound() - count));
                                }
                            }
                        }
                    }
                    if (reqDef.getNodeFilter().getCapabilities() != null && !reqDef.getNodeFilter().getCapabilities().isEmpty()) {
                        Map<String, FilterDefinition> capabilities = reqDef.getNodeFilter().getCapabilities();
                        for (Entry<String, FilterDefinition> filterDefinitionEntry : capabilities.entrySet()) {
                            for (Entry<String, List<PropertyConstraint>> constraintEntry : filterDefinitionEntry.getValue().getProperties().entrySet()) {
                                for (PropertyConstraint constraint : constraintEntry.getValue()) {
                                    NodeTemplate source = topology.getNodeTemplates().get(nodeTempEntry.getKey());
                                    // TODO : get the relationship name
                                    String targetName = source.getRelationships().get("hostedOnCompute").getTarget();
                                    NodeTemplate target = topology.getNodeTemplates().get(targetName);

                                    // Get target nodetype to fond the PD
                                    Map<String, IndexedNodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true);
                                    IndexedNodeType targetIndexedNodeType = nodeTypes.get(source.getName());

                                    for (CapabilityDefinition capabilityDefinition : targetIndexedNodeType.getCapabilities()) {
                                        if (filterDefinitionEntry.getKey().equals(capabilityDefinition.getId())
                                                || filterDefinitionEntry.getKey().equals(capabilityDefinition.getType())) {
                                            AbstractPropertyValue propertyValue = target.getCapabilities().get(capabilityDefinition.getId()).getProperties()
                                                    .get(constraintEntry.getKey());
                                            IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(targetIndexedNodeType.getCapabilities().get(0).getType());

                                            try {
                                                constraint.initialize(toscaType);
                                                // TODO : also check the FunctionPropertyValue
                                                constraint.validate(toscaType, FunctionEvaluator.getScalarValue(propertyValue));
                                            } catch (ConstraintViolationException e) {
                                                task.getRequirementsToImplement().add(
                                                        new RequirementToSatify(reqDef.getId(), constraintEntry.getKey(), reqDef.getLowerBound() - count));
                                            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                                                task.getRequirementsToImplement().add(
                                                        new RequirementToSatify(reqDef.getId(), constraintEntry.getKey(), reqDef.getLowerBound() - count));
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(task.getRequirementsToImplement())) {
                    toReturnTaskList.add(task);
                }
            }
        }
        return toReturnTaskList.isEmpty() ? null : toReturnTaskList;
    }

    private List<PropertiesTask> validateProperties(Topology topology, DeploymentSetup deploymentSetup) {

        List<PropertiesTask> toReturnTaskList = Lists.newArrayList();
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();

        // get the related meta properties
        Map<String, Map<String, String>> mergedMetaProperties = getMergedMetaProperties(deploymentSetup);

        // create task by nodetemplate
        for (Map.Entry<String, NodeTemplate> nodeTempEntry : nodeTemplates.entrySet()) {

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

            // Define a task regarding properties
            PropertiesTask task = new PropertiesTask();
            task.setNodeTemplateName(nodeTempEntry.getKey());
            task.setComponent(relatedIndexedNodeType);
            task.setCode(TaskCode.PROPERTIES);
            task.setProperties(Maps.<TaskLevel, List<String>> newHashMap());

            // Check the properties of node template
            addRequiredPropertyIdToTaskProperties(nodeTemplate.getProperties(), relatedIndexedNodeType.getProperties(), mergedMetaProperties, task);

            // Check relationships PD
            if (nodeTemplate.getRelationships() != null && !nodeTemplate.getRelationships().isEmpty()) {
                Collection<RelationshipTemplate> relationships = nodeTemplate.getRelationships().values();
                for (RelationshipTemplate relationship : relationships) {
                    if (relationship.getProperties() == null || relationship.getProperties().isEmpty()) {
                        continue;
                    }
                    addRequiredPropertyIdToTaskProperties(relationship.getProperties(), getRelationshipPropertyDefinition(topology, nodeTemplate),
                            mergedMetaProperties, task);
                    }
                }

            // Check capabilities PD
            if (nodeTemplate.getCapabilities() != null && !nodeTemplate.getCapabilities().isEmpty()) {
                Collection<Capability> capabilities = nodeTemplate.getCapabilities().values();
                for (Capability capability : capabilities) {
                    if (capability.getProperties() == null || capability.getProperties().isEmpty()) {
                        continue;
                    }
                    addRequiredPropertyIdToTaskProperties(capability.getProperties(), getCapabilitiesPropertyDefinition(topology, nodeTemplate),
                            mergedMetaProperties, task);
                    if (capability.getType().equals(NormativeComputeConstants.SCALABLE_CAPABILITY_TYPE)) {
                        Map<String, AbstractPropertyValue> scalableProperties = capability.getProperties();
                        verifyScalableProperties(scalableProperties, toReturnTaskList, nodeTempEntry.getKey());
                    }
                    }
            }

            if (MapUtils.isNotEmpty(task.getProperties())) {
                if (CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.REQUIRED))
                        || CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.WARNING))) {
                    toReturnTaskList.add(task);
                    }
                }
            }
        return toReturnTaskList.isEmpty() ? null : toReturnTaskList;
        }

    private int verifyScalableProperty(Map<String, AbstractPropertyValue> scalableProperties, String propertyToVerify, Set<String> missingProperties,
            Set<String> errorProperties) {
        String rawValue = FunctionEvaluator.getScalarValue(scalableProperties.get(propertyToVerify));
        if (StringUtils.isEmpty(rawValue)) {
            missingProperties.add(propertyToVerify);
            return -1;
        }
        int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (Exception e) {
            errorProperties.add(propertyToVerify);
            return -1;
        }
        if (value <= 0) {
            errorProperties.add(propertyToVerify);
            return -1;
        }
        return value;
        }

    private void verifyScalableProperties(Map<String, AbstractPropertyValue> scalableProperties, List<PropertiesTask> toReturnTaskList, String nodeTemplateId) {
        Set<String> missingProperties = Sets.newHashSet();
        Set<String> errorProperties = Sets.newHashSet();
        if (MapUtils.isEmpty(scalableProperties)) {
            missingProperties.addAll(Lists.newArrayList(NormativeComputeConstants.SCALABLE_MIN_INSTANCES, NormativeComputeConstants.SCALABLE_MAX_INSTANCES,
                    NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES));

        } else {
            int min = verifyScalableProperty(scalableProperties, NormativeComputeConstants.SCALABLE_MIN_INSTANCES, missingProperties, errorProperties);
            int max = verifyScalableProperty(scalableProperties, NormativeComputeConstants.SCALABLE_MAX_INSTANCES, missingProperties, errorProperties);
            int init = verifyScalableProperty(scalableProperties, NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, missingProperties, errorProperties);
            if (min > 0 && max > 0 && init > 0) {
                if (min > init || min > max) {
                    errorProperties.add(NormativeComputeConstants.SCALABLE_MIN_INSTANCES);
                }
                if (init > max || init < min) {
                    errorProperties.add(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES);
                }
                if (max < min || max < init) {
                    errorProperties.add(NormativeComputeConstants.SCALABLE_MAX_INSTANCES);
                }
                }
            }
        if (!missingProperties.isEmpty()) {
            ScalableTask scalableTask = new ScalableTask(nodeTemplateId);
            scalableTask.getProperties().put(TaskLevel.REQUIRED, Lists.newArrayList(missingProperties));
            toReturnTaskList.add(scalableTask);
        }
        if (!errorProperties.isEmpty()) {
            ScalableTask scalableTask = new ScalableTask(nodeTemplateId);
            scalableTask.getProperties().put(TaskLevel.ERROR, Lists.newArrayList(errorProperties));
            toReturnTaskList.add(scalableTask);
        }
        }

    /**
     * Recover meta properties from cloud, application
     * 
     * @param deploymentSetup
     * @return
     */
    private Map<String, Map<String, String>> getMergedMetaProperties(DeploymentSetup deploymentSetup) {
        ApplicationEnvironment environment = applicationEnvironmentService.getOrFail(deploymentSetup.getEnvironmentId());
        Map<String, Map<String, String>> mergedMetaProperties = Maps.newHashMap();
        Map<String, String> tempPropertyMap = Maps.newHashMap();

        // meta or tags from cloud
        if (environment.getCloudId() != null) {
            Cloud cloud = cloudService.get(environment.getCloudId());
            if (MapUtils.isNotEmpty(cloud.getMetaProperties())) {
                mergedMetaProperties.put(MetaPropertiesTarget.cloud.toString(), cloud.getMetaProperties());
            }
            }
        // meta or tags from application
        if (environment.getApplicationId() != null) {
            Application application = applicationService.getOrFail(environment.getApplicationId());
            Map<String, String> metaProperties = application.getMetaProperties();
            if (MapUtils.isNotEmpty(metaProperties)) {
                tempPropertyMap.putAll(metaProperties);
            }
            Map<String, String> tags = tagService.tagListToMap(application.getTags());
            if (MapUtils.isNotEmpty(tags)) {
                tempPropertyMap.putAll(tags);
            }
            }
        mergedMetaProperties.put(MetaPropertiesTarget.application.toString(), tempPropertyMap);
        // TODO : environment
        return mergedMetaProperties;
        }

    private void addRequiredPropertyIdToTaskProperties(Map<String, AbstractPropertyValue> properties, Map<String, PropertyDefinition> relatedProperties,
            Map<String, Map<String, String>> mergedMetaProperties, PropertiesTask task) {

        for (Map.Entry<String, AbstractPropertyValue> propertyEntry : properties.entrySet()) {

            PropertyDefinition propertyDef = relatedProperties.get(propertyEntry.getKey());
            AbstractPropertyValue value = propertyEntry.getValue();
            String propertyValue = null;
            TaskLevel taskLevel = TaskLevel.WARNING; // default property task level
            boolean isGetInputInternal = false;
            boolean isScalar = false;

            if (value == null) {
                propertyValue = null;
            } else if (value instanceof ScalarPropertyValue) {
                propertyValue = ((ScalarPropertyValue) value).getValue();
                isScalar = true;
            } else if (value instanceof FunctionPropertyValue) {

                // non resolved property from : cloud / environment
                String function = ((FunctionPropertyValue) value).getFunction();

                if (ToscaFunctionConstants.GET_INPUT.equals(function)) {

                    List<String> params = ((FunctionPropertyValue) value).getParameters();
                    String metaPropertyName = params.get(0);
                    isGetInputInternal = InternalMetaProperties.isInternalMeta(metaPropertyName);
                    boolean isTagTarget = InternalMetaProperties.isTag(metaPropertyName);
                    String baseMetaPropertyName = metaPropertyName;

                    if (isGetInputInternal) {
                        // get the thrid part of the propertyname : cloud_meta_XXXXXX_YYY => XXXXXX_YYY
                        baseMetaPropertyName = metaPropertyName.split("_", 3)[2];
                    }

                    // check cloud/environment properties value
                    MetaPropConfiguration metaProperty = metaPropertiesService.getMetaPropertyIdByName(baseMetaPropertyName);
                    MetaPropertiesTarget target = InternalMetaProperties.isCloudMeta(metaPropertyName) ? MetaPropertiesTarget.cloud
                            : MetaPropertiesTarget.application;
                    if (metaProperty != null && MapUtils.isNotEmpty(mergedMetaProperties) || isTagTarget) {
                        if (MapUtils.isNotEmpty(mergedMetaProperties.get(target.toString()))) {
                            // the id in the map could be an UUID for a metaproperty or just a name for a tag
                            String propertyId = isTagTarget ? baseMetaPropertyName : metaProperty.getId();
                            propertyValue = mergedMetaProperties.get(target.toString()).get(propertyId);
                        }
                        }
                    }

            } else {
                throw new InvalidArgumentException("Topology validation only supports scalar value, get_input should be replaced before performing validation");
                }

            if (StringUtils.isBlank(propertyValue)) {
                if (propertyDef.isRequired()) {
                    taskLevel = TaskLevel.REQUIRED;
                    if (!task.getProperties().containsKey(taskLevel)) {
                        task.getProperties().put(taskLevel, Lists.<String> newArrayList());
                    }
                } else { // warning
                    if (!task.getProperties().containsKey(taskLevel)) {
                        task.getProperties().put(taskLevel, Lists.<String> newArrayList());
                    }
                    }
                // add required or warning property
                if (TaskLevel.REQUIRED.equals(taskLevel) || isGetInputInternal || isScalar) {
                    task.getProperties().get(taskLevel).add(propertyEntry.getKey());
                    }
                }
            }
        }

    /**
     * Constructs a TopologyTask list given a Map (node template name => component) and the code
     */
    private <T extends IndexedInheritableToscaElement> List<TopologyTask> getTaskListFromMapArray(Map<String, T[]> components, TaskCode taskCode) {
        List<TopologyTask> taskList = Lists.newArrayList();
        for (Map.Entry<String, T[]> entry : components.entrySet()) {
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
     * Find replacements components for abstract nodes in a Topology
     */
    @SneakyThrows({ IOException.class })
    private List<SuggestionsTask> findReplacementForAbstracts(Topology topology) {
        Map<String, IndexedNodeType> nodeTempNameToAbstractIndexedNodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, true, true);
        Map<String, Map<String, Set<String>>> nodeTemplatesToFilters = Maps.newHashMap();
        for (Map.Entry<String, IndexedNodeType> idntEntry : nodeTempNameToAbstractIndexedNodeTypes.entrySet()) {
            topologyService.processNodeTemplate(topology, Maps.immutableEntry(idntEntry.getKey(), topology.getNodeTemplates().get(idntEntry.getKey())),
                    nodeTemplatesToFilters);
        }
        // processAbstractNodeTemplate(topology, nodeTempNameToAbstractIndexedNodeTypes, nodeTempEntry, nodeTemplatesToFilters);
        // relatedIndexedNodeTypes.put(nodeTempEntry.getKey(), nodeTempNameToAbstractIndexedNodeTypes.get(nodeTempEntry.getValue()));
        return topologyService.searchForNodeTypes(nodeTemplatesToFilters, nodeTempNameToAbstractIndexedNodeTypes);
        }

    /**
     * Validate if a topology is valid for deployment or not
     *
     * @param topology topology to be validated
     * @param deploymentSetup the deployment setup linked to topology
     * @return the validation result
     */
    public TopologyValidationResult validateTopology(Topology topology, DeploymentSetup deploymentSetup, CloudResourceMatcherConfig matcherConfig) {
        TopologyValidationResult dto = new TopologyValidationResult();
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
        // check also CLOUD / ENVIRONMENT meta properties
        List<PropertiesTask> validateProperties = validateProperties(topology, deploymentSetup);
        if (hasOnlyPropertiesWarnings(validateProperties)) {
            dto.addToWarningList(validateProperties);
        } else {
            dto.addToTaskList(validateProperties);
        }

        // Validate that HA groups are respected with current configuration
        if (deploymentSetup != null && matcherConfig != null && MapUtils.isNotEmpty(deploymentSetup.getAvailabilityZoneMapping())) {
            dto.addToWarningList(validateHAGroup(topology, deploymentSetup, matcherConfig));
        }

        dto.setValid(isValidTaskList(dto.getTaskList()));

        return dto;
    }

    private boolean hasOnlyPropertiesWarnings(List<PropertiesTask> properties) {
        if (properties == null) {
            return true;
        }
        for (PropertiesTask task : properties) {
            if (CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.REQUIRED))
                    || CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.ERROR))) {
                return false;
            }
        }
            return true;
        }

    /**
     * Define if a tasks list is valid or not regarding task types
     * 
     * @param taskList
     * @return
     */
    private boolean isValidTaskList(List<TopologyTask> taskList) {
        if (taskList == null) {
            return true;
        }
        for (TopologyTask task : taskList) {
            // checking SuggestionsTask or RequirementsTask
            if (task instanceof SuggestionsTask || task instanceof RequirementsTask || task instanceof PropertiesTask) {
                return false;
            }
            }
            return true;
        }

    private List<TopologyTask> validateHAGroup(Topology topology, DeploymentSetup deploymentSetup, CloudResourceMatcherConfig matcherConfig) {
        AvailabilityZoneAllocator allocator = new AvailabilityZoneAllocator();
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        List<TopologyTask> tasks = Lists.newArrayList();
        List<AllocationError> allocationErrors;
        try {
            Map<String, AvailabilityZone> allocatedZones = allocator.processAllocation(paaSTopology, deploymentSetup, matcherConfig);
            allocationErrors = allocator.validateAllocation(allocatedZones, paaSTopology, deploymentSetup, matcherConfig);
        } catch (AvailabilityZoneConfigurationException e) {
            log.warn("Unable to validate zones allocation due to bad configuration", e);
            tasks.add(new HAGroupTask(null, e.getGroupId(), AllocationErrorCode.CONFIGURATION_ERROR));
            return tasks;
        } catch (Exception e) {
            log.error("Unable to validate zones allocation due to unknown error", e);
            tasks.add(new HAGroupTask(null, null, AllocationErrorCode.UNKNOWN_ERROR));
            return tasks;
            }
        for (AllocationError error : allocationErrors) {
            String nodeId = error.getNodeId();
            tasks.add(new HAGroupTask(nodeId, error.getGroupId(), error.getCode()));
            }
            return tasks;
        }

    private List<TopologyTask> validateAbstractRelationships(Topology topology) {
        Map<String, IndexedRelationshipType[]> abstractIndexedRelationshipTypes = getIndexedRelationshipTypesFromTopology(topology, true);
        return getTaskListFromMapArray(abstractIndexedRelationshipTypes, TaskCode.IMPLEMENT);
        }

    private int countRelationshipsForRequirement(String requirementName, String requirementType, Map<String, RelationshipTemplate> relationships) {
        int count = 0;
        if (relationships == null) {
            return 0;
            }
        for (Map.Entry<String, RelationshipTemplate> relEntry : relationships.entrySet()) {
            if (relEntry.getValue().getRequirementName().equals(requirementName) && relEntry.getValue().getRequirementType().equals(requirementType)) {
                count++;
            }
            }
        return count;
        }

    private Map<String, PropertyDefinition> getCapabilitiesPropertyDefinition(Topology topology, NodeTemplate nodeTemplate) {
        Map<String, PropertyDefinition> relatedProperties = Maps.newTreeMap();

        for (Map.Entry<String, Capability> capabilityEntry : nodeTemplate.getCapabilities().entrySet()) {
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

        for (Map.Entry<String, RelationshipTemplate> relationshipTemplateEntry : nodeTemplate.getRelationships().entrySet()) {
            IndexedRelationshipType indexedRelationshipType = csarRepoSearchService.getRequiredElementInDependencies(IndexedRelationshipType.class,
                    relationshipTemplateEntry.getValue().getType(), topology.getDependencies());
            if (indexedRelationshipType.getProperties() != null && !indexedRelationshipType.getProperties().isEmpty()) {
                relatedProperties.putAll(indexedRelationshipType.getProperties());
            }
            }

        return relatedProperties;
        }

    private void chekCapability(String nodeTemplateName, String capabilityName, NodeTemplate nodeTemplate) {
        boolean capablityExists = false;
        if (nodeTemplate.getCapabilities() != null) {
            for (Map.Entry<String, Capability> capaEntry : nodeTemplate.getCapabilities().entrySet()) {
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
     * Check if the upperBound of a requirement is reached on a node template
     *
     * @param nodeTemplate the node to check for requirement bound
     * @param requirementName the name of the requirement
     * @param dependencies the dependencies of the topology
     * @return true if requirement upper bound is reached, false otherwise
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

        List<RelationshipTemplate> targetRelatedRelationships = topologyServiceCore.getTargetRelatedRelatonshipsTemplate(nodeTemplateName, nodeTemplates);
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

    /**
     * Get the relationships from a topology
     *
     * @param topology topology to be validated
     * @param abstractOnes if only abstract ones should be retrieved
     * @return a map containing node template id --> list of relationship type that this node references
     */
    private Map<String, IndexedRelationshipType[]> getIndexedRelationshipTypesFromTopology(Topology topology, Boolean abstractOnes) {
        Map<String, IndexedRelationshipType[]> indexedRelationshipTypesMap = Maps.newHashMap();
        if (topology.getNodeTemplates() == null) {
            return indexedRelationshipTypesMap;
            }
        for (Map.Entry<String, NodeTemplate> template : topology.getNodeTemplates().entrySet()) {
            if (template.getValue().getRelationships() == null) {
                continue;
            }

            Set<IndexedRelationshipType> indexedRelationshipTypes = Sets.newHashSet();
            for (RelationshipTemplate relTemplate : template.getValue().getRelationships().values()) {
                IndexedRelationshipType indexedRelationshipType = csarRepoSearchService.getElementInDependencies(IndexedRelationshipType.class,
                        relTemplate.getType(), topology.getDependencies());
                if (indexedRelationshipType != null) {
                    if (abstractOnes == null || abstractOnes.equals(indexedRelationshipType.isAbstract())) {
                        indexedRelationshipTypes.add(indexedRelationshipType);
                    }
                } else {
                    throw new NotFoundException("Relationship Type [" + relTemplate.getType() + "] cannot be found");
                    }
                }
            if (indexedRelationshipTypes.size() > 0) {
                indexedRelationshipTypesMap.put(template.getKey(),
                        indexedRelationshipTypes.toArray(new IndexedRelationshipType[indexedRelationshipTypes.size()]));
            }

            }
        return indexedRelationshipTypesMap;
    }

    }
