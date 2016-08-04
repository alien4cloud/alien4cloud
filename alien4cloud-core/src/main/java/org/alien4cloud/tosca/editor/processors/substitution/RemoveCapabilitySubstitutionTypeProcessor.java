package org.alien4cloud.tosca.editor.processors.substitution;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.RemoveCapabilitySubstitutionTypeOperation;
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
public class RemoveCapabilitySubstitutionTypeProcessor implements IEditorOperationProcessor<RemoveCapabilitySubstitutionTypeOperation> {

    @Resource
    private TopologyService topologyService;

    @Resource
    private TopologyServiceCore topologyServiceCore;


    @Override
    public void process(RemoveCapabilitySubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
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
    }
}