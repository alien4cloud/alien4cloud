package org.alien4cloud.tosca.editor.processors.policies;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.policies.RenamePolicyOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.NameValidationUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Processor to rename a policy.
 */
@Slf4j
@Component
public class RenamePolicyProcessor extends AbstractPolicyProcessor<RenamePolicyOperation> {
    @Override
    protected void process(Csar csar, Topology topology, RenamePolicyOperation operation, PolicyTemplate policyTemplate) {

        NameValidationUtils.validateNodeName(operation.getNewName());
        AlienUtils.failIfExists(topology.getPolicies(), operation.getNewName(), "A node template with the given name {} already exists in the topology {}.",
                operation.getNewName(), topology.getId());

        log.debug("Renaming policy template <" + operation.getPolicyName() + "> to <" + operation.getNewName() + "> in the topology <" + topology.getId()
                + "> .");

        policyTemplate.setName(operation.getNewName());
        topology.getPolicies().put(operation.getNewName(), policyTemplate);
        topology.getPolicies().remove(operation.getPolicyName());
    }
}