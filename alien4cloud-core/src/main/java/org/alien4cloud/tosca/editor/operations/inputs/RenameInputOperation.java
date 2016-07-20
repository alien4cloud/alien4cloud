package org.alien4cloud.tosca.editor.operations.inputs;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to update the name of an input.
 */
@Getter
@Setter
public class RenameInputOperation extends AbstractEditorOperation {
    private String inputName;
    private String newInputName;

    @Override
    public String commitMessage() {
        return "rename input artifact <" + inputName + "> to <" + newInputName + ">";
    }
}
