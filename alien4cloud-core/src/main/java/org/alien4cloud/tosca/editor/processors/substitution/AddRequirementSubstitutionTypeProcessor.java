package org.alien4cloud.tosca.editor.processors.substitution;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.AddRequirementSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;

/**
 * Process given requirement as a requirement for the substitution type associated with this topology.
 */
@Component
public class AddRequirementSubstitutionTypeProcessor implements IEditorOperationProcessor<AddRequirementSubstitutionTypeOperation> {

    @Override
    public void process(AddRequirementSubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(operation.getNodeTemplateName())) {
            throw new NotFoundException("Node " + operation.getNodeTemplateName() + " do not exist");
        }

        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(operation.getNodeTemplateName());
        if (nodeTemplate.getRequirements() == null || !nodeTemplate.getRequirements().containsKey(operation.getRequirementId())) {
            throw new NotFoundException("Requirement " + operation.getRequirementId() + " do not exist for node " + operation.getNodeTemplateName());
        }

        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionRequirements = topology.getSubstitutionMapping().getRequirements();
        if (substitutionRequirements == null) {
            substitutionRequirements = Maps.newHashMap();
            topology.getSubstitutionMapping().setRequirements(substitutionRequirements);
        } else if (substitutionRequirements.containsKey(operation.getSubstitutionRequirementId())) {
            // ensure name unicity
            throw new AlreadyExistException(String.format("The substitution requirement <%s> already exists", operation.getSubstitutionRequirementId()));
        }
        substitutionRequirements.put(operation.getSubstitutionRequirementId(), new SubstitutionTarget(operation.getNodeTemplateName(), operation.getRequirementId()));
    }
}