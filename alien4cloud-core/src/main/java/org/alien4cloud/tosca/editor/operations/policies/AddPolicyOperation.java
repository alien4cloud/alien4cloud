package org.alien4cloud.tosca.editor.operations.policies;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to add a policy.
 */
@Getter
@Setter
public class AddPolicyOperation extends AbstractPolicyOperation {
    /** Full id of the policy type (name:version) */
    @NotBlank
    private String policyTypeId;

    @Override
    public String commitMessage() {
        return "add policy <" + getPolicyName() + "> of type <" + policyTypeId + ">";
    }
}