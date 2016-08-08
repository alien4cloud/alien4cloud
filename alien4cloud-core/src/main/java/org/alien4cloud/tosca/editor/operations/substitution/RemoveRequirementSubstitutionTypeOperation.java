package org.alien4cloud.tosca.editor.operations.substitution;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Remove a requirement substitution type.
 */
@Getter
@Setter
public class RemoveRequirementSubstitutionTypeOperation extends AbstractEditorOperation {

    @NotBlank
    private String substitutionRequirementId;

    @Override
    public String commitMessage() {
        return "remove requirement type <" + substitutionRequirementId + ">";
    }
}
