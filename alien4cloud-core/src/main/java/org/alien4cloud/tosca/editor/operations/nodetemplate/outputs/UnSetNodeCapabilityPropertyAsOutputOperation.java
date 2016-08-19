package org.alien4cloud.tosca.editor.operations.nodetemplate.outputs;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to unset a node capability property as output property.
 */
@Getter
@Setter
public class UnSetNodeCapabilityPropertyAsOutputOperation extends SetNodePropertyAsOutputOperation {
    /** Id of the capability */
    private String capabilityName;

    @Override
    public String commitMessage() {
        return "Unset property <" + getPropertyName() + "> of capability <" + capabilityName + ">  of node <" + getNodeName() + "> as output property.";
    }
}
