package org.alien4cloud.tosca.editor.operations.groups;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Add a member to a group. If the group doesn't exist it is automatically created.
 */
@Getter
@Setter
public class AddGroupMemberOperation extends AbstractNodeOperation {
    private String groupName;

    @Override
    public String commitMessage() {
        return "add node <" + getNodeName() + "> to group <" + groupName + ">";
    }
}