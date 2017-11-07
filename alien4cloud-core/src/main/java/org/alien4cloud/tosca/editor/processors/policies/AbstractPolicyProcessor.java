package org.alien4cloud.tosca.editor.processors.policies;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.policies.AbstractPolicyOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.utils.AlienUtils;

/**
 * Process a policy operation.
 */
public abstract class AbstractPolicyProcessor<T extends AbstractPolicyOperation> implements IEditorOperationProcessor<T> {
    @Override
    public void process(Csar csar, Topology topology, T operation) {
        PolicyTemplate policyTemplate = AlienUtils.getOrFail(topology.getPolicies(), operation.getPolicyName(),
                "The policy with name [ {} ] cannot be found in the topology.", operation.getPolicyName());
        process(csar, topology, operation, policyTemplate);
    }

    protected abstract void process(Csar csar, Topology topology, T operation, PolicyTemplate policyTemplate);
}
