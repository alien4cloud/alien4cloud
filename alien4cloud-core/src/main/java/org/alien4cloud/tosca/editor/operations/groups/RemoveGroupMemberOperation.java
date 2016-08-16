package org.alien4cloud.tosca.editor.operations.groups;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Remove a node from a group.
 */
@Getter
@Setter
public class RemoveGroupMemberOperation extends AbstractNodeOperation {
    private String groupName;

    @Override
    public String commitMessage() {
        return "remove member <" + getNodeName() + "> from group <" + groupName + ">";
    }
}
