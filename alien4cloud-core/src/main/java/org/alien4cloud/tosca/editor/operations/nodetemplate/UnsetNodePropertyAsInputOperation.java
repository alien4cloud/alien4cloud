package org.alien4cloud.tosca.editor.operations.nodetemplate;

/**
 * Allows to remove get_input function to the property of a node.
 */
public class UnsetNodePropertyAsInputOperation extends AbstractNodeOperation {
    /** Id of the property */
    private String propertyName;

    @Override
    public String commitMessage() {
        return "property <" + propertyName + "> of node <" + getNodeName() + "> is not tied to an input anymore.";
    }
}