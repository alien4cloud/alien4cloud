package org.alien4cloud.tosca.editor.operations.relationshiptemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the value of a relationship property.
 */
@Getter
@Setter
public class UpdateRelationshipPropertyValueOperation extends AbstractRelationshipOperation {
    private String propertyName;
    private Object propertyValue;

    @Override
    public String commitMessage() {
        if (getPropertyValue() instanceof String) {
            return "update value of property <" + getPropertyName() + "> from relationship <" + getRelationshipName() + "> in node <" + getNodeName() + "> to <"
                    + getPropertyValue() + ">";
        }
        return "update value of property <" + getPropertyName() + "> from relationship <" + getRelationshipName() + "> in node <" + getNodeName() + ">";
    }
}