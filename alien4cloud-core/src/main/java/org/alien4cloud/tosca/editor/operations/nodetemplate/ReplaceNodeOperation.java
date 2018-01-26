package org.alien4cloud.tosca.editor.operations.nodetemplate;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Replace a node template with a compatible node template (inherited or one that fulfill the same used capabilities and requirements etc.).
 */
@Getter
@Setter
public class ReplaceNodeOperation extends AbstractNodeOperation {
    /** Id of the new indexed node type to assign to the node. */
    @NotBlank
    private String newTypeId;
    /** Optional flag to skip auto-completion when adding node. */
    private boolean skipAutoCompletion = false;

    @Override
    public String commitMessage() {
        return "replace type of node <" + getNodeName() + "> to <" + newTypeId + ">";
    }
}
