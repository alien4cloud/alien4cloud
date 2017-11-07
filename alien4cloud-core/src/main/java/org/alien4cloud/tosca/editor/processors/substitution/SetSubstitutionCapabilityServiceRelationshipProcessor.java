package org.alien4cloud.tosca.editor.processors.substitution;

import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionCapabilityServiceRelationshipOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;

/**
 * Set the service relationship to be used to connect to the capability.
 */
@Component
public class SetSubstitutionCapabilityServiceRelationshipProcessor extends SetSubstitutionTargetServiceRelationshipProcessor
        implements IEditorOperationProcessor<SetSubstitutionCapabilityServiceRelationshipOperation> {

    @Override
    public void process(Csar csar, Topology topology, SetSubstitutionCapabilityServiceRelationshipOperation operation) {
        if (topology.getSubstitutionMapping() == null) {
            throw new NotFoundException("The substitution capability with id <" + operation.getSubstitutionCapabilityId() + "> cannot be found.");
        }
        SubstitutionTarget substitutionTarget = safe(topology.getSubstitutionMapping().getCapabilities()).get(operation.getSubstitutionCapabilityId());
        if (substitutionTarget == null) {
            throw new NotFoundException("The substitution capability with id <" + operation.getSubstitutionCapabilityId() + "> cannot be found.");
        }
        super.process(csar, topology, substitutionTarget, operation.getRelationshipType(), operation.getRelationshipVersion());
    }
}