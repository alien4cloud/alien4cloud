package org.alien4cloud.tosca.editor.operations.substitution;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Add a capability substitution type for a topology.
 */
@Getter
@Setter
public class UpdateCapabilitySubstitutionTypeOperation extends AbstractEditorOperation {

    @NotBlank
    private String substitutionCapabilityId;

    @NotBlank
    private String newCapabilityId;

    @Override
    public String commitMessage() {
        return "Change name of the exposed capability from <" + substitutionCapabilityId + "> to <" + newCapabilityId + "> in the substitute type.";
    }
}
