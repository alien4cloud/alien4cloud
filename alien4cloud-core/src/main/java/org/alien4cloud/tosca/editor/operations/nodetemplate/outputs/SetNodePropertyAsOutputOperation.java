package org.alien4cloud.tosca.editor.operations.nodetemplate.outputs;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Operation to set a node property as output property.
 */
@Getter
@Setter
public class SetNodePropertyAsOutputOperation extends AbstractNodeOperation {
    /** The name of the property to set/unset as output. */
    private String propertyName;

    @Override
    public String commitMessage() {
        return "set property <" + getPropertyName() + "> of node <" + getNodeName() + "> as output property.";
    }
}
