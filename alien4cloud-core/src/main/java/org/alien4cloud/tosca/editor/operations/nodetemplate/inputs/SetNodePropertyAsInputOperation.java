package org.alien4cloud.tosca.editor.operations.nodetemplate.inputs;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Allows to affect a get_input function to the property of a node.
 */
@Getter
@Setter
public class SetNodePropertyAsInputOperation extends AbstractNodeOperation {
    /** Id of the property */
    private String propertyName;
    /** The id of the input to associate to the property. */
    private String inputName;

    @Override
    public String commitMessage() {
        return "set property <" + propertyName + "> of node <" + getNodeName() + "> to the input <" + inputName + ">";
    }
}