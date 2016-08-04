package org.alien4cloud.tosca.editor.operations.nodetemplate.outputs;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Operation to unset a node property as output property.
 */
@Getter
@Setter
public class UnSetNodePropertyAsOutputOperation extends AbstractNodeOperation {
    /** The name of the property to unset as output. */
    private String propertyName;

    @Override
    public String commitMessage() {
        return "Unset property <" + getPropertyName() + "> of node <" + getNodeName() + "> as output property.";
    }
}
