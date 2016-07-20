package org.alien4cloud.tosca.editor.operations.groups;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

/**
 * Operation to delete a group from a topology.
 */
@Getter
@Setter
public class DeleteGroupOperation extends AbstractEditorOperation {
    private String groupName;

    @Override
    public String commitMessage() {
        return "delete group <" + groupName + ">";
    }
}
