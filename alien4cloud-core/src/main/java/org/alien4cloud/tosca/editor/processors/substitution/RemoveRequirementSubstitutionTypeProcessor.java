package org.alien4cloud.tosca.editor.processors.substitution;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.RemoveRequirementSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.topology.SubstitutionTarget;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;

/**
 * Delete a group from a topology.
 */
@Component
public class RemoveRequirementSubstitutionTypeProcessor implements IEditorOperationProcessor<RemoveRequirementSubstitutionTypeOperation> {

    @Resource
    private TopologyService topologyService;

    @Resource
    private TopologyServiceCore topologyServiceCore;


    @Override
    public void process(RemoveRequirementSubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
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