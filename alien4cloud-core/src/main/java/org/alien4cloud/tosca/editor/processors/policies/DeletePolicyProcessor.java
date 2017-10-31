package org.alien4cloud.tosca.editor.processors.policies;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.policies.DeletePolicyOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.topology.TopologyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Delete a policy from the topology.
 */
@Slf4j
@Component
public class DeletePolicyProcessor extends AbstractPolicyProcessor<DeletePolicyOperation> {
    @Inject
    private TopologyService topologyService;

    @Override
    protected void process(Csar csar, Topology topology, DeletePolicyOperation operation, PolicyTemplate policyTemplate) {
        log.debug("Removing policy template <" + operation.getPolicyName() + "> of type <" + policyTemplate.getType() + "> from the topology <"
                + topology.getId() + "> .");

        topology.getPolicies().remove(operation.getPolicyName());
        topologyService.unloadType(topology, policyTemplate.getType());
    }
}