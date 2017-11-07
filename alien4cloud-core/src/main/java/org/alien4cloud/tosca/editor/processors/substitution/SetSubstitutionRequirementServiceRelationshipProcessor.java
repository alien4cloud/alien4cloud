package org.alien4cloud.tosca.editor.processors.substitution;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionRequirementServiceRelationshipOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.exception.NotFoundException;
import org.springframework.stereotype.Component;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * Set the service relationship to be used to connect to the capability.
 */
@Component
public class SetSubstitutionRequirementServiceRelationshipProcessor extends SetSubstitutionTargetServiceRelationshipProcessor
        implements IEditorOperationProcessor<SetSubstitutionRequirementServiceRelationshipOperation> {

    @Override
    public void process(Csar csar, Topology topology, SetSubstitutionRequirementServiceRelationshipOperation operation) {
        if (topology.getSubstitutionMapping() == null) {
            throw new NotFoundException("The substitution requirement with id <" + operation.getSubstitutionRequirementId() + "> cannot be found.");
        }
        SubstitutionTarget substitutionTarget = safe(topology.getSubstitutionMapping().getRequirements()).get(operation.getSubstitutionRequirementId());
        if (substitutionTarget == null) {
            throw new NotFoundException("The substitution requirement with id <" + operation.getSubstitutionRequirementId() + "> cannot be found.");
        }
        super.process(csar, topology, substitutionTarget, operation.getRelationshipType(), operation.getRelationshipVersion());
    }
}