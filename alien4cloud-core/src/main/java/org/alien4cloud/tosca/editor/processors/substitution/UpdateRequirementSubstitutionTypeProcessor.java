package org.alien4cloud.tosca.editor.processors.substitution;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.UpdateRequirementSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;

/**
 * Process the addition to a node template to a group. If the group does not exists, it is created.
 */
@Component
    public class UpdateRequirementSubstitutionTypeProcessor implements IEditorOperationProcessor<UpdateRequirementSubstitutionTypeOperation> {

    @Override
    public void process(UpdateRequirementSubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionRequirements = topology.getSubstitutionMapping().getRequirements();
        if (substitutionRequirements == null) {
            throw new NotFoundException("No substitution requirement has been found");
        }
        SubstitutionTarget target = substitutionRequirements.remove(operation.getSubstitutionRequirementId());
        if (target == null) {
            throw new NotFoundException("No substitution requirement has been found for key " + operation.getSubstitutionRequirementId());
        }
        if (substitutionRequirements.containsKey(operation.getNewRequirementId())) {
            throw new AlreadyExistException(
                    String.format("Can not rename from <%s> to <%s> since requirement <%s> already exists", operation.getSubstitutionRequirementId(), operation.getNewRequirementId(), operation.getNewRequirementId()));
        }
        substitutionRequirements.put(operation.getNewRequirementId(), target);
    }
}