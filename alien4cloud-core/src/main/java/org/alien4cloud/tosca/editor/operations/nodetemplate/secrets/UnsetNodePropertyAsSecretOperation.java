package org.alien4cloud.tosca.editor.operations.nodetemplate.secrets;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to remove get_secret function to the property of a node.
 */
@Getter
@Setter
public class UnsetNodePropertyAsSecretOperation extends AbstractNodeOperation {
    /** Id of the property */
    private String propertyName;

    @Override
    public String commitMessage() {
        return "property <" + propertyName + "> of node <" + getNodeName() + "> is not tied as secret anymore.";
    }
}