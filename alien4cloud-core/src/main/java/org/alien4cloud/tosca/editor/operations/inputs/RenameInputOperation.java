package org.alien4cloud.tosca.editor.operations.inputs;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to update the name of an input.
 */
@Getter
@Setter
public class RenameInputOperation extends AbstractInputOperation {
    private String newInputName;

    @Override
    public String commitMessage() {
        return "rename input artifact <" + getInputName() + "> to <" + newInputName + ">";
    }
}
