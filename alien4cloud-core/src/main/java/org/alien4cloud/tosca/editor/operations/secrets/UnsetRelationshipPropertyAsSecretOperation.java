package org.alien4cloud.tosca.editor.operations.secrets;

import org.alien4cloud.tosca.editor.operations.relationshiptemplate.AbstractRelationshipOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to remove get_secret function from the property of a relationship.
 */
@Getter
@Setter
public class UnsetRelationshipPropertyAsSecretOperation extends AbstractRelationshipOperation {

    /** Id of the property */
    private String propertyName;
    /** The path of the secret */
    private String secretPath;

    @Override
    public String commitMessage() {
        return "Unset property <" + propertyName + "> of relationship <" + getRelationshipName() + "> in node <" + getNodeName() + "> with a secret <path: " + secretPath
                + ">.";
    }
}
