package org.alien4cloud.tosca.editor.operations.inputs;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

/**
 * Operation to remove an input (and remove all associations to it).
 */
@Getter
@Setter
public class RemoveInputOperation extends AbstractEditorOperation {
    /** The name of the input to add in the topology. */
    private String inputName;
}
