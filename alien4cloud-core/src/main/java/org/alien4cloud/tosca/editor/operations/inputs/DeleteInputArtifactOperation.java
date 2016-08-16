package org.alien4cloud.tosca.editor.operations.inputs;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to remove an input artifact (and remove all associations to it).
 */
@Getter
@Setter
public class DeleteInputArtifactOperation extends AbstractEditorOperation {
    /** The name of the input to add in the topology. */
    private String inputName;

    @Override
    public String commitMessage() {
        return "remove input artifact <" + inputName + ">";
    }
}