package org.alien4cloud.tosca.editor.operations.nodetemplate.outputs;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to unset a node attribute as output.
 */
@Getter
@Setter
public class UnSetNodeAttributeAsOutputOperation extends AbstractNodeOperation {
    /** The name of the attribute */
    private String attributeName;

    @Override
    public String commitMessage() {
        return "unset attribute <" + getAttributeName() + "> of node <" + getNodeName() + "> as output.";
    }
}
