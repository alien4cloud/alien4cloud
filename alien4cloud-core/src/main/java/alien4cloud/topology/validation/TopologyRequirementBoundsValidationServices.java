package alien4cloud.topology.validation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Requirement;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.topology.task.RequirementToSatisfy;
import alien4cloud.topology.task.RequirementsTask;
import alien4cloud.topology.task.TaskCode;

import static org.alien4cloud.tosca.utils.NodeTemplateUtils.countRelationshipsForRequirement;

/**
 * Performs validation of the requirements and capabilities bounds.
 */
@Component
public class TopologyRequirementBoundsValidationServices {
    @Resource
    private IToscaTypeSearchService toscaTypeSearchService;

    /**
     * Check if the upperBound of a requirement is reached on a node template
     *
     * @param nodeTemplate the node to check for requirement bound
     * @param requirementName the name of the requirement
     * @param dependencies the dependencies of the topology
     * @return true if requirement upper bound is reached, false otherwise
     */
    public boolean isRequirementUpperBoundReachedForSource(NodeTemplate nodeTemplate, String requirementName, Set<CSARDependency> dependencies) {
        NodeType relatedIndexedNodeType = toscaTypeSearchService.getRequiredElementInDependencies(NodeType.class, nodeTemplate.getType(), dependencies);
        Requirement requirement = nodeTemplate.getRequirements().get(requirementName);
        if (nodeTemplate.getRelationships() == null || nodeTemplate.getRelationships().isEmpty()) {
            return false;
        }

        RequirementDefinition requirementDefinition = getRequirementDefinition(relatedIndexedNodeType.getRequirements(), requirementName,
                requirement.getType());

        if (requirementDefinition.getUpperBound() == Integer.MAX_VALUE) {
            return false;
        }

        int count = countRelationshipsForRequirement(nodeTemplate, requirementDefinition);

        return count >= requirementDefinition.getUpperBound();
    }

    /**
     * Perform validation of requirements bounds/occurences for the given topology.
     * 
     * @param topology The topology to check
     * @return A list of validation errors (tasks to be done to make the topology compliant).
     */
    public List<RequirementsTask> validateRequirementsLowerBounds(Topology topology) {
        List<RequirementsTask> toReturnTaskList = Lists.newArrayList();
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        for (Map.Entry<String, NodeTemplate> nodeTempEntry : nodeTemplates.entrySet()) {
            NodeTemplate nodeTemp = nodeTempEntry.getValue();
            if (nodeTemp.getRequirements() == null) {
                continue;
            }
            NodeType relatedIndexedNodeType = toscaTypeSearchService.getRequiredElementInDependencies(NodeType.class, nodeTemp.getType(),
                    topology.getDependencies());
            // do pass if abstract node
            if (relatedIndexedNodeType.isAbstract()) {
                continue;
            }
            RequirementsTask task = new RequirementsTask();
            task.setNodeTemplateName(nodeTempEntry.getKey());
            task.setCode(TaskCode.SATISFY_LOWER_BOUND);
            task.setComponent(relatedIndexedNodeType);
            task.setRequirementsToImplement(Lists.<RequirementToSatisfy> newArrayList());
            if (CollectionUtils.isNotEmpty(relatedIndexedNodeType.getRequirements())) {
                for (RequirementDefinition reqDef : relatedIndexedNodeType.getRequirements()) {
                    int count = countRelationshipsForRequirement(nodeTemp, reqDef);
                    if (count < reqDef.getLowerBound()) {
                        task.getRequirementsToImplement().add(new RequirementToSatisfy(reqDef.getId(), reqDef.getType(), reqDef.getLowerBound() - count));
                    }
                }
                if (CollectionUtils.isNotEmpty(task.getRequirementsToImplement())) {
                    toReturnTaskList.add(task);
                }
            }
        }
        return toReturnTaskList.isEmpty() ? null : toReturnTaskList;
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
}
