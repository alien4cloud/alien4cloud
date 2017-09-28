package org.alien4cloud.tosca.editor.operations.policies;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the targets of a policy.
 */
@Getter
@Setter
public class UpdatePolicyTargetsOperation extends AbstractPolicyOperation {
    private String[] targets;

    @Override
    public String commitMessage() {
        return "update targets from policy <" + getPolicyName() + "> to <" + targets + ">";
    }
}