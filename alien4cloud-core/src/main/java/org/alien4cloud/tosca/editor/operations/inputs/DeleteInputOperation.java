package org.alien4cloud.tosca.editor.operations.inputs;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to remove an input (and remove all associations to it).
 */
@Getter
@Setter
public class DeleteInputOperation extends AbstractInputOperation {
    @Override
    public String commitMessage() {
        return "remove input <" + getInputName() + ">";
    }
}