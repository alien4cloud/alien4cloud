package org.alien4cloud.tosca.editor.operations.secrets;

import org.alien4cloud.tosca.editor.operations.policies.AbstractPolicyOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to affect a get_secret function to the property of a policy.
 */
@Getter
@Setter
public class SetPolicyPropertyAsSecretOperation extends AbstractPolicyOperation {

    private String propertyName;
    private String secretPath;

    @Override
    public String commitMessage() {
        return "set property <" + propertyName + "> of policy <" + getPolicyName() + "> as secret <path:" + secretPath + ">";
    }
}
