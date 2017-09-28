package org.alien4cloud.tosca.editor.operations.policies;

/**
 * Operation to delete a policy from the topology.
 */
public class DeletePolicyOperation extends AbstractPolicyOperation {
    @Override
    public String commitMessage() {
        return "delete policy <" + getPolicyName() + ">";
    }
}
