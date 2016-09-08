package org.alien4cloud.tosca.editor.operations.relationshiptemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to rebuild a relationship template.
 */
@Getter
@Setter
public class RebuildRelationshipOperation extends AbstractRelationshipOperation {
    @Override
    public String commitMessage() {
        return "rebuild relationship <" + getRelationshipName() + "> from the source node <" + getNodeName() + "> ";
    }
}
