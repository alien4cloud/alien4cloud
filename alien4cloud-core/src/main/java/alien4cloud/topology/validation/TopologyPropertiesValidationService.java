package alien4cloud.topology.validation;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.ScalableTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import alien4cloud.tosca.normative.NormativeComputeConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Performs validation of the properties
 */
@Component
@Slf4j
public class TopologyPropertiesValidationService {
    @Resource
    private IToscaTypeSearchService csarRepoSearchService;

    /**
     * Validate that the properties values in the topology are matching the property definitions (required & constraints).
     * Skips properties defined as get_input
     *
     * @param topology The actual topology to validate.
     * @return A list tasks to be done to make this topology valid.
     */
    public List<PropertiesTask> validateStaticProperties(Topology topology) {
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
        for (Map.Entry<String, NodeTemplate> nodeTempEntry : nodeTemplates.entrySet()) {
            NodeTemplate nodeTemplate = nodeTempEntry.getValue();
            NodeType relatedIndexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, nodeTemplate.getType(),
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
            if (MapUtils.isNotEmpty(nodeTemplate.getProperties())) {
                addRequiredPropertyIdToTaskProperties(null, nodeTemplate.getProperties(), relatedIndexedNodeType.getProperties(), task, skipInputProperties);
            }

            // Check relationships PD
            for (Map.Entry<String, RelationshipTemplate> relationshipEntry : safe(nodeTemplate.getRelationships()).entrySet()) {
                RelationshipTemplate relationship = relationshipEntry.getValue();
                if (relationship.getProperties() == null || relationship.getProperties().isEmpty()) {
                    continue;
                }
                addRequiredPropertyIdToTaskProperties("relationships[" + relationshipEntry.getKey() + "]", relationship.getProperties(),
                        getRelationshipPropertyDefinition(topology, nodeTemplate), task, skipInputProperties);
            }
            for (Map.Entry<String, Capability> capabilityEntry : safe(nodeTemplate.getCapabilities()).entrySet()) {
                Capability capability = capabilityEntry.getValue();
                if (capability.getProperties() == null || capability.getProperties().isEmpty()) {
                    continue;
                }
                addRequiredPropertyIdToTaskProperties("capabilities[" + capabilityEntry.getKey() + "]", capability.getProperties(),
                        getCapabilitiesPropertyDefinition(topology, nodeTemplate), task, skipInputProperties);
                if (capability.getType().equals(NormativeComputeConstants.SCALABLE_CAPABILITY_TYPE)) {
                    Map<String, AbstractPropertyValue> scalableProperties = capability.getProperties();
                    verifyScalableProperties(scalableProperties, toReturnTaskList, nodeTempEntry.getKey(), skipInputProperties);
                }
            }

            if (MapUtils.isNotEmpty(task.getProperties())) {
                toReturnTaskList.add(task);
            }
        }
        return toReturnTaskList.isEmpty() ? null : toReturnTaskList;
    }

    private Map<String, PropertyDefinition> getCapabilitiesPropertyDefinition(Topology topology, NodeTemplate nodeTemplate) {
        Map<String, PropertyDefinition> relatedProperties = Maps.newTreeMap();

        for (Map.Entry<String, Capability> capabilityEntry : nodeTemplate.getCapabilities().entrySet()) {
            CapabilityType indexedCapabilityType = csarRepoSearchService.getRequiredElementInDependencies(CapabilityType.class,
                    capabilityEntry.getValue().getType(), topology.getDependencies());
            if (indexedCapabilityType.getProperties() != null && !indexedCapabilityType.getProperties().isEmpty()) {
                relatedProperties.putAll(indexedCapabilityType.getProperties());
            }
        }

        return relatedProperties;
    }

    private Map<String, PropertyDefinition> getRelationshipPropertyDefinition(Topology topology, NodeTemplate nodeTemplate) {
        Map<String, PropertyDefinition> relatedProperties = Maps.newTreeMap();

        for (Map.Entry<String, RelationshipTemplate> relationshipTemplateEntry : nodeTemplate.getRelationships().entrySet()) {
            RelationshipType indexedRelationshipType = csarRepoSearchService.getRequiredElementInDependencies(RelationshipType.class,
                    relationshipTemplateEntry.getValue().getType(), topology.getDependencies());
            if (indexedRelationshipType.getProperties() != null && !indexedRelationshipType.getProperties().isEmpty()) {
                relatedProperties.putAll(indexedRelationshipType.getProperties());
            }
        }

        return relatedProperties;
    }

    private void verifyScalableProperties(Map<String, AbstractPropertyValue> scalableProperties, List<PropertiesTask> toReturnTaskList, String nodeTemplateId,
            boolean skipInputProperties) {
        Set<String> missingProperties = Sets.newHashSet();
        Set<String> errorProperties = Sets.newHashSet();
        if (skipInputProperties) {
            for (Entry<String, AbstractPropertyValue> entry : scalableProperties.entrySet()) {
                if (entry.getValue() instanceof FunctionPropertyValue) {
                    return;
                }
            }
        }
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

    private int verifyScalableProperty(Map<String, AbstractPropertyValue> scalableProperties, String propertyToVerify, Set<String> missingProperties,
            Set<String> errorProperties) {
        String rawValue = null;
        try {
            rawValue = FunctionEvaluator.getScalarValue(scalableProperties.get(propertyToVerify));
        } catch (NotSupportedException e1) {
            // the value is a function (get_input normally), this means the input is not yet filled.
        }
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

    private void addRequiredPropertyError(PropertiesTask task, String propertyName) {
        if (!task.getProperties().containsKey(TaskLevel.REQUIRED)) {
            task.getProperties().put(TaskLevel.REQUIRED, Lists.<String> newArrayList());
        }
        task.getProperties().get(TaskLevel.REQUIRED).add(propertyName);
    }

    private void addRequiredPropertyIdToTaskProperties(String prefix, Map<String, AbstractPropertyValue> properties,
            Map<String, PropertyDefinition> relatedProperties, PropertiesTask task, boolean skipInputProperties) {
        for (Map.Entry<String, AbstractPropertyValue> propertyEntry : properties.entrySet()) {
            PropertyDefinition propertyDef = relatedProperties.get(propertyEntry.getKey());
            String propertyErrorKey = prefix == null ? propertyEntry.getKey() : prefix + "." + propertyEntry.getKey();
            AbstractPropertyValue value = propertyEntry.getValue();
            if (propertyDef != null && propertyDef.isRequired()) {
                if (value == null) {
                    addRequiredPropertyError(task, propertyErrorKey);
                } else if (value instanceof ScalarPropertyValue) {
                    String propertyValue = ((ScalarPropertyValue) value).getValue();
                    if (StringUtils.isBlank(propertyValue)) {
                        addRequiredPropertyError(task, propertyErrorKey);
                    }
                } else if (value instanceof ComplexPropertyValue) {
                    Map<String, Object> mapValue = ((ComplexPropertyValue) value).getValue();
                    if (MapUtils.isEmpty(mapValue)) {
                        addRequiredPropertyError(task, propertyErrorKey);
                    }
                } else if (value instanceof ListPropertyValue) {
                    List<Object> listValue = ((ListPropertyValue) value).getValue();
                    if (listValue.isEmpty()) {
                        addRequiredPropertyError(task, propertyErrorKey);
                    }
                } else if (skipInputProperties) {
                    // this is a get_input funtion.
                    // get_input Will be validated later on
                    continue;
                } else {
                    addRequiredPropertyError(task, propertyErrorKey);
                }
            }
        }
    }

}