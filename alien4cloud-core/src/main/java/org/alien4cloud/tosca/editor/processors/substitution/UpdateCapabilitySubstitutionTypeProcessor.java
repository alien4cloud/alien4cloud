package org.alien4cloud.tosca.editor.processors.substitution;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.UpdateCapabilitySubstitutionTypeOperation;
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
public class UpdateCapabilitySubstitutionTypeProcessor implements IEditorOperationProcessor<UpdateCapabilitySubstitutionTypeOperation> {

    @Override
    public void process(UpdateCapabilitySubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionCapabilities = topology.getSubstitutionMapping().getCapabilities();
        if (substitutionCapabilities == null) {
            throw new NotFoundException("No substitution capabilities has been found");
        }
        SubstitutionTarget target = substitutionCapabilities.remove(operation.getSubstitutionCapabilityId());
        if (target == null) {
            throw new NotFoundException("No substitution capability has been found for key " + operation.getSubstitutionCapabilityId());
        }
        if (substitutionCapabilities.containsKey(operation.getNewCapabilityId())) {
            throw new AlreadyExistException(
                    String.format("Can not rename from <%s> to <%s> since capability <%s> already exists", operation.getSubstitutionCapabilityId(), operation.getNewCapabilityId(), operation.getNewCapabilityId()));
        }
        substitutionCapabilities.put(operation.getNewCapabilityId(), target);
    }
}