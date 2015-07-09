package alien4cloud.topology.validation;

import alien4cloud.model.components.*;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.topology.task.RequirementToSatify;
import alien4cloud.topology.task.RequirementsTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * Created by lucboutier on 09/07/15.
 */
public class NodeFilterValidationService {
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
                        for (Map.Entry<String, List<PropertyConstraint>> propertyEntry : properties.entrySet()) {
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
                        for (Map.Entry<String, FilterDefinition> filterDefinitionEntry : capabilities.entrySet()) {
                            for (Map.Entry<String, List<PropertyConstraint>> constraintEntry : filterDefinitionEntry.getValue().getProperties().entrySet()) {
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

}
