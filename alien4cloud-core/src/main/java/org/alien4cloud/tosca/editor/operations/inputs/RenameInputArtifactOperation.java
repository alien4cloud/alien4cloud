package org.alien4cloud.tosca.editor.operations.inputs;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

/**
 *
 */
@Getter
@Setter
public class RenameInputArtifactOperation extends AbstractEditorOperation {
    private String inputName;
    private String newInputName;

    @Override
    public String commitMessage() {
        return "rename input artifact <" + inputName + "> to <" + newInputName + ">";
    }
}