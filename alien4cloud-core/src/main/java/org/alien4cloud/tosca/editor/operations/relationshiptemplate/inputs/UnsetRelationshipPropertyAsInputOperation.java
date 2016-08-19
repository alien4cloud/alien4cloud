package org.alien4cloud.tosca.editor.operations.relationshiptemplate.inputs;

import org.alien4cloud.tosca.editor.operations.relationshiptemplate.AbstractRelationshipOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to remove get_input function to the property of a node.
 */
@Getter
@Setter
public class UnsetRelationshipPropertyAsInputOperation extends AbstractRelationshipOperation {
    /** Id of the property */
    private String propertyName;

    @Override
    public String commitMessage() {
        return "property <" + propertyName + "> from relationship <" + getRelationshipName() + "> in node <" + getNodeName()
                + "> is not tied to an input anymore.";
    }
}