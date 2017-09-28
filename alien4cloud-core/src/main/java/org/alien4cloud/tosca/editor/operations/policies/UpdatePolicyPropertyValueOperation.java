package org.alien4cloud.tosca.editor.operations.policies;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the value of a policy.
 */
@Getter
@Setter
public class UpdatePolicyPropertyValueOperation extends AbstractPolicyOperation {
    private String propertyName;
    private Object propertyValue;

    @Override
    public String commitMessage() {
        if (propertyValue instanceof String) {
            return "update value of property <" + propertyName + "> in policy <" + getPolicyName() + "> to <" + propertyValue + ">";
        }
        return "update value of property <" + propertyName + "> in policy <" + getPolicyName() + ">";
    }
}