package org.alien4cloud.tosca.editor.operations.inputs;

import alien4cloud.model.components.PropertyDefinition;
import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

/**
 * Operation to add an input to the topology.
 */
@Getter
@Setter
public class AddInputOperation extends AbstractInputOperation {
    /** The property definition to associate to the input. */
    private PropertyDefinition propertyDefinition;

    @Override
    public String commitMessage() {
        return "add new input with name <" + getInputName() + "> and type <" + propertyDefinition.getType() + ">";
    }
}
