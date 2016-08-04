package org.alien4cloud.tosca.editor.operations.substitution;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Remove a requirement substitution type.
 */
@Getter
@Setter
public class RemoveRequirementSubstitutionTypeOperation extends AbstractTopologyTemplateOperation {

    @NotBlank
    private String substitutionRequirementId;


    @Override
    public String commitMessage() {
        return "remove requirement type <" + substitutionRequirementId + "> for topology <" + getTopologyId() + ">";
    }
}
