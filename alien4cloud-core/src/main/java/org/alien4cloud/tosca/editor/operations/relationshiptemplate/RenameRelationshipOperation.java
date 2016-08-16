package org.alien4cloud.tosca.editor.operations.relationshiptemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Rename a relationship in a topology template.
 */
@Getter
@Setter
public class RenameRelationshipOperation extends AbstractRelationshipOperation {
    /** New name for the relationship template. */
    private String newRelationshipName;

    @Override
    public String commitMessage() {
        return "rename relationship <" + getRelationshipName() + "> in node <" + getNodeName() + "> to <" + newRelationshipName + ">";
    }
}