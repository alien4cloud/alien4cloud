package org.alien4cloud.tosca.editor.processors.policies;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.policies.UpdatePolicyTargetsOperation;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.utils.AlienUtils;

/**
 * Updates the list of targets of a policy.
 */
@Component
public class UpdatePolicyTargetsProcessor extends AbstractPolicyProcessor<UpdatePolicyTargetsOperation> {
    @Override
    protected void process(UpdatePolicyTargetsOperation operation, PolicyTemplate policyTemplate) {
        Topology topology = EditionContextManager.getTopology();
        for (String target : operation.getTargets()) {
            AlienUtils.getOrFail(topology.getNodeTemplates(), target,
                    "The node with name <{}> and assigned as policy <{}> target cannot be found in the topology.", target, policyTemplate.getName());
        }
        policyTemplate.setTargets(operation.getTargets());
    }
}