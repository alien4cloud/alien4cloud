package alien4cloud.topology.validation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Component;

import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.ScalableTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import alien4cloud.tosca.normative.NormativeComputeConstants;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Performs validation of the properties
 */
@Component
@Slf4j
public class TopologyPropertiesValidationService {
    @Resource
    private CSARRepositorySearchService csarRepoSearchService;

    /**
     * Validate that the properties values in the topology are matching the property definitions (required & constraints).
     * Skips properties defined as get_input
     *
     * @param topology The actual topology to validate.
     * @return A list tasks to be done to make this topology valid.
     */
    public List<PropertiesTask> validatePropertiesSkipInputs(Topology topology) {
        return validateProperties(topology, true);
    }

    /**
     * Validate that the All properties (including dynamic properties (get_input )) values in the topology are matching the property definitions (required &
     * constraints).
     *
     * @param topology The actual topology to validate.
     * @return A list tasks to be done to make this topology valid.
     */
    public List<PropertiesTask> validateAllProperties(Topology topology) {
        return validateProperties(topology, false);
    }

    /**
     * Validate properties.
     *
     * @param topology
     * @param skipInputProperties whether to skip input properties validation or not. This is in case inputs are not yet processed
     * @return
     */
    private List<PropertiesTask> validateProperties(Topology topology, boolean skipInputProperties) {
        List<PropertiesTask> toReturnTaskList = Lists.newArrayList();
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();

        // create task by nodetemplate
        for(Map.Entry<String, NodeTemplate> nodeTempEntry : nodeTemplates.entrySet()) {
            NodeTemplate nodeTemplate = nodeTempEntry.getValue();
            IndexedNodeType relatedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                    topology.getDependencies());
            // do pass if abstract node
            if(relatedIndexedNodeType.isAbstract()) {
                continue;
            }

            // Define a task regarding properties
            PropertiesTask task = new PropertiesTask();
            task.setNodeTemplateName(nodeTempEntry.getKey());
            task.setComponent(relatedIndexedNodeType);
            task.setCode(TaskCode.PROPERTIES);
            task.setProperties(Maps.<TaskLevel, List<String>> newHashMap());

            // Check the properties of node template
            if(MapUtils.isNotEmpty(nodeTemplate.getProperties())) {
                addRequiredPropertyIdToTaskProperties(nodeTemplate.getProperties(), relatedIndexedNodeType.getProperties(), task, skipInputProperties);
            }

            // Check relationships PD
            if(MapUtils.isNotEmpty(nodeTemplate.getRelationships())) {
                Collection<RelationshipTemplate> relationships = nodeTemplate.getRelationships().values();
                for(RelationshipTemplate relationship : relationships) {
                    if(relationship.getProperties() == null || relationship.getProperties().isEmpty()) {
                        continue;
                    }
                    addRequiredPropertyIdToTaskProperties(relationship.getProperties(), getRelationshipPropertyDefinition(topology, nodeTemplate), task,
                            skipInputProperties);
                }
            }

            // Check capabilities PD
            if(MapUtils.isNotEmpty(nodeTemplate.getCapabilities())) {
                Collection<Capability> capabilities = nodeTemplate.getCapabilities().values();
                for(Capability capability : capabilities) {
                    if(capability.getProperties() == null || capability.getProperties().isEmpty()) {
                        continue;
                    }
                    addRequiredPropertyIdToTaskProperties(capability.getProperties(), getCapabilitiesPropertyDefinition(topology, nodeTemplate), task,
                            skipInputProperties);
                    if(capability.getType().equals(NormativeComputeConstants.SCALABLE_CAPABILITY_TYPE)) {
                        Map<String, AbstractPropertyValue> scalableProperties = capability.getProperties();
                        verifyScalableProperties(scalableProperties, toReturnTaskList, nodeTempEntry.getKey(), skipInputProperties);
                    }
                }
            }

            if(MapUtils.isNotEmpty(task.getProperties())) {
                // why verify this????
                if(CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.REQUIRED))
                        || CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.WARNING))) {
                    toReturnTaskList.add(task);
                }
            }
        }
        return toReturnTaskList.isEmpty() ? null : toReturnTaskList;
    }

    private Map<String, PropertyDefinition> getCapabilitiesPropertyDefinition(Topology topology, NodeTemplate nodeTemplate) {
        Map<String, PropertyDefinition> relatedProperties = Maps.newTreeMap();

        for(Map.Entry<String, Capability> capabilityEntry : nodeTemplate.getCapabilities().entrySet()) {
            IndexedCapabilityType indexedCapabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class, capabilityEntry
                    .getValue().getType(), topology.getDependencies());
            if(indexedCapabilityType.getProperties() != null && !indexedCapabilityType.getProperties().isEmpty()) {
                relatedProperties.putAll(indexedCapabilityType.getProperties());
            }
        }

        return relatedProperties;
    }

    private Map<String, PropertyDefinition> getRelationshipPropertyDefinition(Topology topology, NodeTemplate nodeTemplate) {
        Map<String, PropertyDefinition> relatedProperties = Maps.newTreeMap();

        for(Map.Entry<String, RelationshipTemplate> relationshipTemplateEntry : nodeTemplate.getRelationships().entrySet()) {
            IndexedRelationshipType indexedRelationshipType = csarRepoSearchService.getRequiredElementInDependencies(IndexedRelationshipType.class,
                    relationshipTemplateEntry.getValue().getType(), topology.getDependencies());
            if(indexedRelationshipType.getProperties() != null && !indexedRelationshipType.getProperties().isEmpty()) {
                relatedProperties.putAll(indexedRelationshipType.getProperties());
            }
        }

        return relatedProperties;
    }

    private void verifyScalableProperties(Map<String, AbstractPropertyValue> scalableProperties, List<PropertiesTask> toReturnTaskList, String nodeTemplateId,
            boolean skipInputProperties) {
        Set<String> missingProperties = Sets.newHashSet();
        Set<String> errorProperties = Sets.newHashSet();
        if(skipInputProperties) {
            for(Entry<String, AbstractPropertyValue> entry : scalableProperties.entrySet()) {
                if(entry.getValue() instanceof FunctionPropertyValue) {
                    return;
                }
            }
        }
        if(MapUtils.isEmpty(scalableProperties)) {
            missingProperties.addAll(Lists.newArrayList(NormativeComputeConstants.SCALABLE_MIN_INSTANCES, NormativeComputeConstants.SCALABLE_MAX_INSTANCES,
                    NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES));

        } else {
            int min = verifyScalableProperty(scalableProperties, NormativeComputeConstants.SCALABLE_MIN_INSTANCES, missingProperties, errorProperties);
            int max = verifyScalableProperty(scalableProperties, NormativeComputeConstants.SCALABLE_MAX_INSTANCES, missingProperties, errorProperties);
            int init = verifyScalableProperty(scalableProperties, NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, missingProperties, errorProperties);
            if(min > 0 && max > 0 && init > 0) {
                if(min > init || min > max) {
                    errorProperties.add(NormativeComputeConstants.SCALABLE_MIN_INSTANCES);
                }
                if(init > max || init < min) {
                    errorProperties.add(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES);
                }
                if(max < min || max < init) {
                    errorProperties.add(NormativeComputeConstants.SCALABLE_MAX_INSTANCES);
                }
            }
        }
        if(!missingProperties.isEmpty()) {
            ScalableTask scalableTask = new ScalableTask(nodeTemplateId);
            scalableTask.getProperties().put(TaskLevel.REQUIRED, Lists.newArrayList(missingProperties));
            toReturnTaskList.add(scalableTask);
        }
        if(!errorProperties.isEmpty()) {
            ScalableTask scalableTask = new ScalableTask(nodeTemplateId);
            scalableTask.getProperties().put(TaskLevel.ERROR, Lists.newArrayList(errorProperties));
            toReturnTaskList.add(scalableTask);
        }
    }

    private int verifyScalableProperty(Map<String, AbstractPropertyValue> scalableProperties, String propertyToVerify, Set<String> missingProperties,
            Set<String> errorProperties) {
        String rawValue = null;
        try {
            rawValue = FunctionEvaluator.getScalarValue(scalableProperties.get(propertyToVerify));
        } catch(NotSupportedException e1) {
            // the value is a function (get_input normally), this means the input is not yet filled.
        }
        if(StringUtils.isEmpty(rawValue)) {
            missingProperties.add(propertyToVerify);
            return -1;
        }
        int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch(Exception e) {
            errorProperties.add(propertyToVerify);
            return -1;
        }
        if(value <= 0) {
            errorProperties.add(propertyToVerify);
            return -1;
        }
        return value;
    }

    private void addRequiredPropertyIdToTaskProperties(Map<String, AbstractPropertyValue> properties, Map<String, PropertyDefinition> relatedProperties,
            PropertiesTask task, boolean skipInputProperties) {
        for(Map.Entry<String, AbstractPropertyValue> propertyEntry : properties.entrySet()) {

            PropertyDefinition propertyDef = relatedProperties.get(propertyEntry.getKey());
            AbstractPropertyValue value = propertyEntry.getValue();
            String propertyValue = null;
            TaskLevel taskLevel = TaskLevel.WARNING; // default property task level
            boolean isGetInputInternal = false;
            boolean isScalar = false;

            if(value == null) {
                propertyValue = null;
            } else if(value instanceof ScalarPropertyValue) {
                propertyValue = ((ScalarPropertyValue) value).getValue();
                isScalar = true;
            } else {
                // this is a get_input funtion.
                if(skipInputProperties) {
                    // get_input Will be validated later on
                    continue;
                }
            }

            if(StringUtils.isBlank(propertyValue)) {
                if(propertyDef.isRequired()) {
                    taskLevel = TaskLevel.REQUIRED;
                    if(!task.getProperties().containsKey(taskLevel)) {
                        task.getProperties().put(taskLevel, Lists.<String> newArrayList());
                    }
                } else { // warning
                    if(!task.getProperties().containsKey(taskLevel)) {
                        task.getProperties().put(taskLevel, Lists.<String> newArrayList());
                    }
                }
                // add required or warning property
                // ??? why this check?
                if(TaskLevel.REQUIRED.equals(taskLevel) || isGetInputInternal || isScalar) {
                    task.getProperties().get(taskLevel).add(propertyEntry.getKey());
                }
            }
        }
    }

}