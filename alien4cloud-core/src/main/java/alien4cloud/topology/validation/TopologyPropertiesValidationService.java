package alien4cloud.topology.validation;

import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.services.PropertyDefaultValueService;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.ScalableTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.PropertyUtil;
import alien4cloud.utils.services.ConstraintPropertyService;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.utils.FunctionEvaluator;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * Performs validation of the properties
 */
@Component
@Slf4j
public class TopologyPropertiesValidationService {

    @Autowired
    private PropertyDefaultValueService defaultValueService;

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
            NodeType relatedIndexedNodeType = ToscaContext.get(NodeType.class, nodeTemplate.getType());
            // do pass if abstract node
            if (relatedIndexedNodeType.isAbstract()) {
                continue;
            }
            validateNodeTemplate(toReturnTaskList, relatedIndexedNodeType, nodeTemplate, nodeTempEntry.getKey(), skipInputProperties);
        }
        return toReturnTaskList.isEmpty() ? null : toReturnTaskList;
    }

    public void validateNodeTemplate(List<PropertiesTask> toReturnTaskList, NodeType relatedIndexedNodeType, NodeTemplate nodeTemplate, String nodeTempalteName,
            boolean skipInputProperties) {
        // Define a task regarding properties
        PropertiesTask task = new PropertiesTask();
        task.setNodeTemplateName(nodeTempalteName);
        task.setComponent(relatedIndexedNodeType);
        task.setCode(TaskCode.PROPERTIES);
        task.setProperties(Maps.newHashMap());

        // Check the properties of node template
        if (MapUtils.isNotEmpty(nodeTemplate.getProperties())) {
            var fedProperties = defaultValueService.feedDefaultValues(nodeTemplate);
            addRequiredPropertyIdToTaskProperties(null, fedProperties, relatedIndexedNodeType.getProperties(), task, skipInputProperties);
        }

        // Check relationships PD
        for (var relationshipEntry : safe(nodeTemplate.getRelationships()).entrySet()) {
            RelationshipTemplate relationship = relationshipEntry.getValue();
            if (relationship.getProperties() == null || relationship.getProperties().isEmpty()) {
                continue;
            }
            addRequiredPropertyIdToTaskProperties("relationships[" + relationshipEntry.getKey() + "]", relationship.getProperties(),
                    safe(ToscaContext.getOrFail(RelationshipType.class, relationshipEntry.getValue().getType()).getProperties()), task, skipInputProperties);
        }
        for (var capabilityEntry : safe(nodeTemplate.getCapabilities()).entrySet()) {
            var fedProperties = defaultValueService.feedDefaultValuesForCapability(nodeTemplate,capabilityEntry.getKey());

            Capability capability = capabilityEntry.getValue();
            if (capability.getProperties() == null || capability.getProperties().isEmpty()) {
                continue;
            }
            addRequiredPropertyIdToTaskProperties("capabilities[" + capabilityEntry.getKey() + "]", capability.getProperties(),
                    safe(ToscaContext.getOrFail(CapabilityType.class, capabilityEntry.getValue().getType()).getProperties()), task, skipInputProperties);
            if (capability.getType().equals(NormativeCapabilityTypes.SCALABLE)) {
                Map<String, AbstractPropertyValue> scalableProperties = capability.getProperties();
                verifyScalableProperties(scalableProperties, toReturnTaskList, nodeTempalteName, skipInputProperties);
            }
        }

        if (MapUtils.isNotEmpty(task.getProperties())) {
            toReturnTaskList.add(task);
        }
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
            rawValue = PropertyUtil.getScalarValue(scalableProperties.get(propertyToVerify));
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
            if (propertyDef != null) {

                if (value instanceof ComplexPropertyValue) {
                    Map<String, Object> mapValue = ((ComplexPropertyValue) value).getValue();

                    try {
                        ConstraintPropertyService.checkPropertyConstraint(propertyEntry.getKey(), mapValue, propertyDef, key -> {
                            addRequiredPropertyError(task, prefix == null ? key : prefix + "." + key);
                        });
                    } catch(ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
                        // Value probably not matching the definition (shoud not happen in this context)
                        // Nothing to do here, we re just checking for required properties
                    }
                }

                if (propertyDef.isRequired()) {
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
                    } else if (FunctionEvaluator.containGetSecretFunction(value)) {
                        // this is a get_secret function, we should not validate the get_secret here
                        continue;
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
}