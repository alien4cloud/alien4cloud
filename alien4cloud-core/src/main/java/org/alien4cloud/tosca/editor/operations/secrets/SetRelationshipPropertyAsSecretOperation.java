package org.alien4cloud.tosca.editor.operations.secrets;

import org.alien4cloud.tosca.editor.operations.relationshiptemplate.AbstractRelationshipOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to affect a get_secret function to the property of a relationship.
 */
@Getter
@Setter
public class SetRelationshipPropertyAsSecretOperation extends AbstractRelationshipOperation {
    /** Id of the property */
    private String propertyName;
    /** The path of the secret */
    private String secretPath;

    @Override
    public String commitMessage() {
        return "set property <" + propertyName + "> of relationship <" + getRelationshipName() + "> in node <" + getNodeName() + "> with a secret <path: " + secretPath
                + ">.";
    }
}
