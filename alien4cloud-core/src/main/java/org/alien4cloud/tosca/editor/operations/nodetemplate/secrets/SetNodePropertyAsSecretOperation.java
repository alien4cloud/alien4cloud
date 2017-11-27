package org.alien4cloud.tosca.editor.operations.nodetemplate.secrets;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to affect a get_secret function to the property of a node.
 */
@Getter
@Setter
public class SetNodePropertyAsSecretOperation extends AbstractNodeOperation {
    /** Id of the property */
    private String propertyName;
    /** The path of the secret */
    private String secretPath;

    @Override
    public String commitMessage() {
        return "set property <" + propertyName + "> of node <" + getNodeName() + "> as secret <" + secretPath + ">";
    }
}