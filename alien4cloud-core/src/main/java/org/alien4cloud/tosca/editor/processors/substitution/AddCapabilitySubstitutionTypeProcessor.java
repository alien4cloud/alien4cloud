package org.alien4cloud.tosca.editor.processors.substitution;

import java.util.Map;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.AddCapabilitySubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;

/**
 * Process given capability as a capability for the substitution type associated with this topology.
 */
@Component
public class AddCapabilitySubstitutionTypeProcessor implements IEditorOperationProcessor<AddCapabilitySubstitutionTypeOperation> {

    @Override
    public void process(AddCapabilitySubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(operation.getNodeTemplateName())) {
            throw new NotFoundException("Node " + operation.getNodeTemplateName() + " do not exist");
        }

        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(operation.getNodeTemplateName());
        if (nodeTemplate.getCapabilities() == null || !nodeTemplate.getCapabilities().containsKey(operation.getCapabilityId())) {
            throw new NotFoundException("Capability " + operation.getCapabilityId() + " do not exist for node " + operation.getNodeTemplateName());
        }

        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        Map<String, SubstitutionTarget> substitutionCapabilities = topology.getSubstitutionMapping().getCapabilities();
        if (substitutionCapabilities == null) {
            substitutionCapabilities = Maps.newHashMap();
            topology.getSubstitutionMapping().setCapabilities(substitutionCapabilities);
        } else if (substitutionCapabilities.containsKey(operation.getSubstitutionCapabilityId())) {
            // ensure name unicity
            throw new AlreadyExistException(String.format("The substitution capability <%s> already exists", operation.getSubstitutionCapabilityId()));
        }
        substitutionCapabilities.put(operation.getSubstitutionCapabilityId(), new SubstitutionTarget(operation.getNodeTemplateName(), operation.getCapabilityId()));
    }
}