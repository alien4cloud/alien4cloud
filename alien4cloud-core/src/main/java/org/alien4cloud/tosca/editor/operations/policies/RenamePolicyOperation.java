package org.alien4cloud.tosca.editor.operations.policies;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to rename a policy.
 */
@Getter
@Setter
public class RenamePolicyOperation extends AbstractPolicyOperation {
    @NotBlank
    private String newName;

    @Override
    public String commitMessage() {
        return "rename policy <" + getPolicyName() + "> to <" + newName + ">";
    }
}
