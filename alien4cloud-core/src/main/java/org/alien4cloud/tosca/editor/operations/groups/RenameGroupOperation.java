package org.alien4cloud.tosca.editor.operations.groups;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to rename a group in a topology template.
 */
@Getter
@Setter
public class RenameGroupOperation extends AbstractEditorOperation {
    private String groupName;
    private String newGroupName;

    @Override
    public String commitMessage() {
        return "rename group <" + groupName + "> to <" + newGroupName + ">";
    }
}
