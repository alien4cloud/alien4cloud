package org.alien4cloud.tosca.editor.operations.inputs;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Abstract operation to manage inputs.
 */
@Getter
@Setter
public abstract class AbstractInputOperation extends AbstractEditorOperation {
    /** The name of the input to add/remove or rename in the topology. */
    private String inputName;
}
