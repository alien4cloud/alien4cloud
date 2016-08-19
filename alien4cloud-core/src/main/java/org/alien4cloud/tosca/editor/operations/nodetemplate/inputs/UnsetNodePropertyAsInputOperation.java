package org.alien4cloud.tosca.editor.operations.nodetemplate.inputs;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to remove get_input function to the property of a node.
 */
@Getter
@Setter
public class UnsetNodePropertyAsInputOperation extends AbstractNodeOperation {
    /** Id of the property */
    private String propertyName;

    @Override
    public String commitMessage() {
        return "property <" + propertyName + "> of node <" + getNodeName() + "> is not tied to an input anymore.";
    }
}