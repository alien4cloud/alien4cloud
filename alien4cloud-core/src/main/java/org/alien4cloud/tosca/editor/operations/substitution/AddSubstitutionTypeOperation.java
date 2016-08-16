package org.alien4cloud.tosca.editor.operations.substitution;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Add a substitution type for a topology.
 */
@Getter
@Setter
public class AddSubstitutionTypeOperation extends AbstractEditorOperation {

    @NotBlank
    private String elementId;

    @Override
    public String commitMessage() {
        return "add type <" + elementId + ">";
    }
}
