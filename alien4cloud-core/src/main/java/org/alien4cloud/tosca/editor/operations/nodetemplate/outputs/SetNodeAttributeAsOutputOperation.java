package org.alien4cloud.tosca.editor.operations.nodetemplate.outputs;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Operation to set a node attribute as output.
 */
@Getter
@Setter
public class SetNodeAttributeAsOutputOperation extends AbstractNodeOperation {
    /** The name of the attribute*/
    private String attributeName;

    @Override
    public String commitMessage() {
        return "set attribute <" + getAttributeName() + "> of node <" + getNodeName() + "> as output.";
    }
}
