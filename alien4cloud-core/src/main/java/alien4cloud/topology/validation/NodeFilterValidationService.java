package alien4cloud.topology.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.model.components.FilterDefinition;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.NodeFilter;
import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.RequirementDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.task.NodeFilterConstraintViolation;
import alien4cloud.topology.task.NodeFilterToSatisfy;
import alien4cloud.topology.task.NodeFiltersTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Performs validation of node filters for all relationship of topology.
 */
@Component
public class NodeFilterValidationService {
    @Resource
    private CSARRepositorySearchService csarRepoSearchService;
    @Resource
    private TopologyServiceCore topologyServiceCore;

    private Map<String, RequirementDefinition> getRequirementsAsMap(IndexedNodeType nodeType) {
        Map<String, RequirementDefinition> requirementDefinitionMap = Maps.newHashMap();
        for (RequirementDefinition definition : nodeType.getRequirements()) {
            requirementDefinitionMap.put(definition.getId(), definition);
        }
        return requirementDefinitionMap;
    }

    /**
     * Performs validation of the node filters to check that relationships targets the filter requirements.
     */
    public List<NodeFiltersTask> validateRequirementFilters(Topology topology) {
        List<NodeFiltersTask> toReturnTaskList = Lists.newArrayList();
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        Map<String, IndexedNodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true);
        Map<String, IndexedCapabilityType> capabilityTypes = topologyServiceCore.getIndexedCapabilityTypesFromTopology(topology);
        for (Map.Entry<String, NodeTemplate> nodeTempEntry : nodeTemplates.entrySet()) {
            Map<String, RelationshipTemplate> relationshipsMap = nodeTempEntry.getValue().getRelationships();
            if (relationshipsMap == null || relationshipsMap.isEmpty()) {
                continue;
            }
            IndexedNodeType sourceNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTempEntry.getValue().getType(),
                    topology.getDependencies());
            if (sourceNodeType.isAbstract()) {
                continue;
            }

            NodeFiltersTask task = new NodeFiltersTask();
            task.setNodeTemplateName(nodeTempEntry.getKey());
            task.setCode(TaskCode.NODE_FILTER_INVALID);
            task.setComponent(sourceNodeType);
            task.setNodeFiltersToSatisfy(Lists.<NodeFilterToSatisfy> newArrayList());

            validateFiltersForNode(sourceNodeType, relationshipsMap, topology, nodeTypes, capabilityTypes, task);

            if (!task.getNodeFiltersToSatisfy().isEmpty()) {
                toReturnTaskList.add(task);
            }
        }
        return toReturnTaskList.isEmpty() ? null : toReturnTaskList;
    }

    private void validateFiltersForNode(IndexedNodeType sourceNodeType, Map<String, RelationshipTemplate> relationshipsMap, Topology topology,
            Map<String, IndexedNodeType> nodeTypes, Map<String, IndexedCapabilityType> capabilityTypes, NodeFiltersTask task) {
        Map<String, RequirementDefinition> requirementDefinitionMap = getRequirementsAsMap(sourceNodeType);
        for (Map.Entry<String, RelationshipTemplate> relationshipEntry : relationshipsMap.entrySet()) {
            RequirementDefinition requirementDefinition = requirementDefinitionMap.get(relationshipEntry.getValue().getRequirementName());
            NodeFilter nodeFilter = requirementDefinition.getNodeFilter();
            if (nodeFilter != null) {
                NodeTemplate targetNode = topology.getNodeTemplates().get(relationshipEntry.getValue().getTarget());
                IndexedNodeType targetType = nodeTypes.get(relationshipEntry.getValue().getTarget());

                NodeFilterToSatisfy nodeFilterToSatisfy = new NodeFilterToSatisfy();
                nodeFilterToSatisfy.setRelationshipName(relationshipEntry.getKey());
                nodeFilterToSatisfy.setTargetName(targetNode.getName());

                validateNodeFilter(nodeFilter, targetNode, targetType, capabilityTypes, nodeFilterToSatisfy);

                if (!nodeFilterToSatisfy.getViolatedConstraints().isEmpty() || !nodeFilterToSatisfy.getMissingCapabilities().isEmpty()) {
                    task.getNodeFiltersToSatisfy().add(nodeFilterToSatisfy);
                }
            }
        }
    }

    private void validateNodeFilter(NodeFilter nodeFilter, NodeTemplate target, IndexedNodeType targetType, Map<String, IndexedCapabilityType> capabilityTypes,
            NodeFilterToSatisfy nodeFilterToSatisfy) {
        Map<String, List<NodeFilterConstraintViolation>> violatedConstraints = validateNodeFilterProperties(nodeFilter, target, targetType);
        nodeFilterToSatisfy.setViolatedConstraints(violatedConstraints);

        validateNodeFilterCapabilities(nodeFilter, target, targetType, capabilityTypes, nodeFilterToSatisfy);
    }

    private Map<String, List<NodeFilterConstraintViolation>> validateNodeFilterProperties(NodeFilter nodeFilter, NodeTemplate target, IndexedNodeType targetType) {
        if (nodeFilter.getProperties() == null || nodeFilter.getProperties().isEmpty()) {
            return null;
        }

        Map<String, List<PropertyConstraint>> propertyFilters = nodeFilter.getProperties();
        Map<String, AbstractPropertyValue> propertyValues = target.getProperties();
        return validatePropertyFilters(propertyFilters, propertyValues, targetType.getProperties());
    }

    private Map<String, List<NodeFilterConstraintViolation>> validatePropertyFilters(Map<String, List<PropertyConstraint>> propertyFilters,
            Map<String, AbstractPropertyValue> propertyValues, Map<String, PropertyDefinition> propertyDefinitionMap) {
        Map<String, List<NodeFilterConstraintViolation>> violatedConstraintsMap = Maps.newHashMap();

        for (Map.Entry<String, List<PropertyConstraint>> propertyEntry : propertyFilters.entrySet()) {
            List<NodeFilterConstraintViolation> violatedConstraints = Lists.newArrayList();

            for (PropertyConstraint constraint : propertyEntry.getValue()) {
                if (!propertyDefinitionMap.containsKey(propertyEntry.getKey())) {
                    continue;
                }

                AbstractPropertyValue value = propertyValues.get(propertyEntry.getKey());
                // the constraint need to be initiazed with the type of the property (to check that actual value type matches the definition type).
                IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(propertyDefinitionMap.get(propertyEntry.getKey()).getType());
                try {
                    constraint.initialize(toscaType);
                    String propertyValue;
                    if (value == null) {
                        propertyValue = null;
                    } else if (value instanceof ScalarPropertyValue) {
                        propertyValue = ((ScalarPropertyValue) value).getValue();
                    } else {
                        throw new InvalidArgumentException(
                                "Topology validation only supports scalar value, get_input should be replaced before performing validation");
                    }
                    constraint.validate(toscaType, propertyValue);
                } catch (ConstraintViolationException e) {
                    violatedConstraints.add(new NodeFilterConstraintViolation(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR, e.getMessage(), e
                            .getConstraintInformation()));
                } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                    violatedConstraints.add(new NodeFilterConstraintViolation(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR, e.getMessage(), null));
                }
            }

            if (!violatedConstraints.isEmpty()) {
                violatedConstraintsMap.put(propertyEntry.getKey(), violatedConstraints);
            }
        }

        return violatedConstraintsMap;
    }

    private void validateNodeFilterCapabilities(NodeFilter nodeFilter, NodeTemplate target, IndexedNodeType targetType,
            Map<String, IndexedCapabilityType> capabilityTypes, NodeFilterToSatisfy nodeFilterToSatisfy) {
        nodeFilterToSatisfy.setMissingCapabilities(Lists.<String> newArrayList());
        if (nodeFilter.getCapabilities() == null || nodeFilter.getCapabilities().isEmpty()) {
            return;
        }

        Map<String, FilterDefinition> capabilities = nodeFilter.getCapabilities();
        for (Map.Entry<String, FilterDefinition> filterDefinitionEntry : capabilities.entrySet()) {
            String capabilityName = filterDefinitionEntry.getKey();
            CapabilityDefinition definition = getCapabilityDefinition(targetType, capabilityName);

            if (definition == null) {
                nodeFilterToSatisfy.getMissingCapabilities().add(capabilityName);
                continue;
            }
            IndexedCapabilityType capabilityType = capabilityTypes.get(definition.getType());

            Map<String, List<NodeFilterConstraintViolation>> violations = validatePropertyFilters(filterDefinitionEntry.getValue().getProperties(), target
                    .getCapabilities().get(definition.getId()).getProperties(), capabilityType.getProperties());
            if (nodeFilterToSatisfy.getViolatedConstraints() == null) {
                nodeFilterToSatisfy.setViolatedConstraints(new HashMap<String, List<NodeFilterConstraintViolation>>());
            }
            nodeFilterToSatisfy.getViolatedConstraints().putAll(violations);
        }
    }

    private CapabilityDefinition getCapabilityDefinition(IndexedNodeType targetType, String filterCapabilityKey) {
        for (CapabilityDefinition capabilityDefinition : targetType.getCapabilities()) {
            if (filterCapabilityKey.equals(capabilityDefinition.getId()) || filterCapabilityKey.equals(capabilityDefinition.getType())) {
                return capabilityDefinition;
            }
        }
        return null;
    }
}
