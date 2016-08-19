package org.alien4cloud.tosca.editor.operations.nodetemplate;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the name of a node template.
 */
@Getter
@Setter
public class RenameNodeOperation extends AbstractNodeOperation {
    @NotBlank
    private String newName;

    @Override
    public String commitMessage() {
        return "rename node <" + getNodeName() + "> to <" + newName + ">";
    }
}
