package org.alien4cloud.tosca.editor.operations.nodetemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the value of a property of the capability of a node template.
 */
@Getter
@Setter
public class UpdateCapabilityPropertyValueOperation extends UpdateNodePropertyValueOperation {
    private String capabilityName;

    @Override
    public String commitMessage() {
        if (getPropertyValue() instanceof String) {
            return "update value of property <" + getPropertyName() + "> from capability <" + getCapabilityName() + "> in node <" + getNodeName() + "> to <" + getPropertyValue() + ">";
        }
        return "update value of property <" + getPropertyName() + "> from capability <" + getCapabilityName() + "> in node <" + getNodeName() + ">";
    }
}
