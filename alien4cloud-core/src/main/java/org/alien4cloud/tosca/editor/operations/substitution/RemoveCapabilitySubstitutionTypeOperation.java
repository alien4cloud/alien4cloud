package org.alien4cloud.tosca.editor.operations.substitution;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Remove a capability substitution type.
 */
@Getter
@Setter
public class RemoveCapabilitySubstitutionTypeOperation extends AbstractEditorOperation {

    @NotBlank
    private String substitutionCapabilityId;

    @Override
    public String commitMessage() {
        return "remove capability type <" + substitutionCapabilityId + ">";
    }
}
