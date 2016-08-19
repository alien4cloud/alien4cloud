package org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs;

import org.alien4cloud.tosca.editor.operations.relationshiptemplate.AbstractRelationshipOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to affect a get_input function to the property of a node.
 */
@Getter
@Setter
public class SetRelationshipPropertyAsInputOperation extends AbstractRelationshipOperation {
    /** Id of the property */
    private String propertyName;
    /** The id of the input to associate to the property. */
    private String inputName;

    @Override
    public String commitMessage() {
        return "set property <" + propertyName + "> of relationship <" + getRelationshipName() + "> in node <" + getNodeName() + "> to the input <" + inputName
                + ">.";
    }
}