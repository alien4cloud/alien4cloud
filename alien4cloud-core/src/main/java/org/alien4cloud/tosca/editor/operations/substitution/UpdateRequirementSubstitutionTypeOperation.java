package org.alien4cloud.tosca.editor.operations.substitution;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Update a requirement substitution type for a topology.
 */
@Getter
@Setter
public class UpdateRequirementSubstitutionTypeOperation extends AbstractTopologyTemplateOperation {

    @NotBlank
    private String substitutionRequirementId;

    @NotBlank
    private String newRequirementId;


    @Override
    public String commitMessage() {
        return "update requirement type substitution <" + substitutionRequirementId + "> to <" + newRequirementId + "> for the topology <" + getTopologyId() + ">";
    }
}
