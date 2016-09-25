package alien4cloud.topology.validation;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.task.NodeFilterConstraintViolation;
import alien4cloud.topology.task.NodeFilterToSatisfy;
import alien4cloud.topology.task.NodeFiltersTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.NodeFilterToSatisfy.Violations;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

/**
 * Performs validation of node filters for all relationship of topology.
 */
@Component
public class NodeFilterValidationService {
    @Inject
    private IToscaTypeSearchService csarRepoSearchService;
    @Resource
    private TopologyServiceCore topologyServiceCore;

    private Map<String, RequirementDefinition> getRequirementsAsMap(NodeType nodeType) {
        Map<String, RequirementDefinition> requirementDefinitionMap = Maps.newHashMap();
        for (RequirementDefinition definition : nodeType.getRequirements()) {
            requirementDefinitionMap.put(definition.getId(), definition);
        }
        return requirementDefinitionMap;
    }

    public List<NodeFiltersTask> validateStaticRequirementFilters(Topology topology) {
        return validateRequirementFilters(topology, true);
    }

    public List<NodeFiltersTask> validateAllRequirementFilters(Topology topology) {
        return validateRequirementFilters(topology, false);
    }

    /**
     * Performs validation of the node filters to check that relationships targets the filter requirements.
     */
    private List<NodeFiltersTask> validateRequirementFilters(Topology topology, boolean skipInputs) {
        List<NodeFiltersTask> toReturnTaskList = Lists.newArrayList();
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        Map<String, NodeType> nodeTypes = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true, true);
        Map<String, CapabilityType> capabilityTypes = topologyServiceCore.getIndexedCapabilityTypesFromTopology(topology);
        for (Map.Entry<String, NodeTemplate> nodeTempEntry : nodeTemplates.entrySet()) {
            Map<String, RelationshipTemplate> relationshipsMap = nodeTempEntry.getValue().getRelationships();
            if (relationshipsMap == null || relationshipsMap.isEmpty()) {
                continue;
            }
            NodeType sourceNodeType = csarRepoSearchService.getRequiredElementInDependencies(NodeType.class, nodeTempEntry.getValue().getType(),
                    topology.getDependencies());
            if (sourceNodeType.isAbstract()) {
                continue;
            }

            NodeFiltersTask task = new NodeFiltersTask();
            task.setNodeTemplateName(nodeTempEntry.getKey());
            task.setCode(TaskCode.NODE_FILTER_INVALID);
            task.setComponent(sourceNodeType);
            task.setNodeFiltersToSatisfy(Lists.<NodeFilterToSatisfy> newArrayList());

            validateFiltersForNode(sourceNodeType, relationshipsMap, topology, nodeTypes, capabilityTypes, task, skipInputs);

            if (!task.getNodeFiltersToSatisfy().isEmpty()) {
                toReturnTaskList.add(task);
            }
        }
        return toReturnTaskList.isEmpty() ? null : toReturnTaskList;
    }

    private void validateFiltersForNode(NodeType sourceNodeType, Map<String, RelationshipTemplate> relationshipsMap, Topology topology,
                                        Map<String, NodeType> nodeTypes, Map<String, CapabilityType> capabilityTypes, NodeFiltersTask task, boolean skipInputs) {
        Map<String, RequirementDefinition> requirementDefinitionMap = getRequirementsAsMap(sourceNodeType);
        for (Map.Entry<String, RelationshipTemplate> relationshipEntry : relationshipsMap.entrySet()) {
            RequirementDefinition requirementDefinition = requirementDefinitionMap.get(relationshipEntry.getValue().getRequirementName());
            NodeFilter nodeFilter = requirementDefinition.getNodeFilter();
            if (nodeFilter != null) {
                NodeTemplate targetNode = topology.getNodeTemplates().get(relationshipEntry.getValue().getTarget());
                NodeType targetType = nodeTypes.get(relationshipEntry.getValue().getTarget());

                NodeFilterToSatisfy nodeFilterToSatisfy = new NodeFilterToSatisfy();
                nodeFilterToSatisfy.setRelationshipName(relationshipEntry.getKey());
                nodeFilterToSatisfy.setTargetName(targetNode.getName());

                validateNodeFilter(nodeFilter, targetNode, targetType, capabilityTypes, nodeFilterToSatisfy, skipInputs);

                if (!nodeFilterToSatisfy.getViolations().isEmpty() || !nodeFilterToSatisfy.getMissingCapabilities().isEmpty()) {
                    task.getNodeFiltersToSatisfy().add(nodeFilterToSatisfy);
                }
            }
        }
    }

    private void validateNodeFilter(NodeFilter nodeFilter, NodeTemplate target, NodeType targetType, Map<String, CapabilityType> capabilityTypes,
                                    NodeFilterToSatisfy nodeFilterToSatisfy, boolean skipInputs) {
        List<Violations> violations = validateNodeFilterProperties(nodeFilter, target, targetType, skipInputs);
        nodeFilterToSatisfy.setViolations(violations);

        validateNodeFilterCapabilities(nodeFilter, target, targetType, capabilityTypes, nodeFilterToSatisfy, skipInputs);
    }

    private List<Violations> validateNodeFilterProperties(NodeFilter nodeFilter, NodeTemplate target, NodeType targetType, boolean skipInputs) {
        if (nodeFilter.getProperties() == null || nodeFilter.getProperties().isEmpty()) {
            return null;
        }

        Map<String, List<PropertyConstraint>> propertyFilters = nodeFilter.getProperties();
        Map<String, AbstractPropertyValue> propertyValues = target.getProperties();
        return validatePropertyFilters(propertyFilters, propertyValues, targetType.getProperties(), skipInputs);
    }

    private List<Violations> validatePropertyFilters(Map<String, List<PropertyConstraint>> propertyFilters, Map<String, AbstractPropertyValue> propertyValues,
            Map<String, PropertyDefinition> propertyDefinitionMap, boolean skipInputs) {
        List<Violations> violations = Lists.newArrayList();

        for (Map.Entry<String, List<PropertyConstraint>> propertyEntry : propertyFilters.entrySet()) {
            Violations violation = new Violations(propertyEntry.getKey());
            List<NodeFilterConstraintViolation> violatedConstraints = Lists.newArrayList();
            violation.violatedConstraints = violatedConstraints;
            AbstractPropertyValue value = propertyValues.get(propertyEntry.getKey());
            String propertyValue = null;
            if (value == null) {
                propertyValue = null;
            } else if (value instanceof ScalarPropertyValue) {
                propertyValue = ((ScalarPropertyValue) value).getValue();
            } else {
                if (skipInputs) {
                    continue;
                }
                if (FunctionEvaluator.isGetInput((FunctionPropertyValue) value)) {
                    violation.relatedInput = ((FunctionPropertyValue) value).getElementNameToFetch();
                }
            }

            for (PropertyConstraint constraint : propertyEntry.getValue()) {
                if (!propertyDefinitionMap.containsKey(propertyEntry.getKey())) {
                    continue;
                }

                // the constraint need to be initiazed with the type of the property (to check that actual value type matches the definition type).
                IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(propertyDefinitionMap.get(propertyEntry.getKey()).getType());
                try {
                    constraint.initialize(toscaType);
                    constraint.validate(toscaType, propertyValue);
                } catch (ConstraintViolationException e) {
                    violatedConstraints.add(
                            new NodeFilterConstraintViolation(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR, e.getMessage(), e.getConstraintInformation()));
                } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                    violatedConstraints.add(new NodeFilterConstraintViolation(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR, e.getMessage(), null));
                }
            }

            if (!violatedConstraints.isEmpty()) {
                violations.add(violation);
            }
        }

        return violations;
    }

    private void validateNodeFilterCapabilities(NodeFilter nodeFilter, NodeTemplate target, NodeType targetType,
                                                Map<String, CapabilityType> capabilityTypes, NodeFilterToSatisfy nodeFilterToSatisfy, boolean skipInputs) {
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
            CapabilityType capabilityType = capabilityTypes.get(definition.getType());

            List<Violations> violations = validatePropertyFilters(filterDefinitionEntry.getValue().getProperties(),
                    target.getCapabilities().get(definition.getId()).getProperties(), capabilityType.getProperties(), skipInputs);
            if (nodeFilterToSatisfy.getViolations() == null) {
                nodeFilterToSatisfy.setViolations(Lists.<Violations> newArrayList());
            }
            nodeFilterToSatisfy.getViolations().addAll(violations);
        }
    }

    private CapabilityDefinition getCapabilityDefinition(NodeType targetType, String filterCapabilityKey) {
        for (CapabilityDefinition capabilityDefinition : targetType.getCapabilities()) {
            if (filterCapabilityKey.equals(capabilityDefinition.getId()) || filterCapabilityKey.equals(capabilityDefinition.getType())) {
                return capabilityDefinition;
            }
        }
        return null;
    }
}
