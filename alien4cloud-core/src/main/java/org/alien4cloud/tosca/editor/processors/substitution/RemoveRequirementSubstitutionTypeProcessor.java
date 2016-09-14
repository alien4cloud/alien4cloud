package org.alien4cloud.tosca.editor.processors.substitution;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.RemoveRequirementSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;

/**
 * Delete a group from a topology.
 */
@Component
public class RemoveRequirementSubstitutionTypeProcessor implements IEditorOperationProcessor<RemoveRequirementSubstitutionTypeOperation> {

    @Override
    public void process(RemoveRequirementSubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionRequirements = topology.getSubstitutionMapping().getRequirements();
        if (substitutionRequirements == null) {
            throw new NotFoundException("No requirements has been found");
        }
        SubstitutionTarget target = substitutionRequirements.remove(operation.getSubstitutionRequirementId());
        if (target == null) {
            throw new NotFoundException("No substitution requirement has been found for key " + operation.getSubstitutionRequirementId());
        }
    }
}