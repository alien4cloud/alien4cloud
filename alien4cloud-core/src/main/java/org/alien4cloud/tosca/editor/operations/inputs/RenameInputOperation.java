package org.alien4cloud.tosca.editor.operations.inputs;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

/**
 * Operation to update the name of an input.
 */
@Getter
@Setter
public class RenameInputOperation extends AbstractEditorOperation {
    private String inputName;
    private String newInputName;
}
