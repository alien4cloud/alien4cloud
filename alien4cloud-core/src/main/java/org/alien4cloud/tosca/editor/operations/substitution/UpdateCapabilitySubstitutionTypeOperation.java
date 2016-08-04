package org.alien4cloud.tosca.editor.operations.substitution;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Add a capability substitution type for a topology.
 */
@Getter
@Setter
public class UpdateCapabilitySubstitutionTypeOperation extends AbstractTopologyTemplateOperation {

    @NotBlank
    private String substitutionCapabilityId;

    @NotBlank
    private String newCapabilityId;


    @Override
    public String commitMessage() {
        return "update capability type substitution <" + substitutionCapabilityId + "> to <" + newCapabilityId + "> for the topology <" + getTopologyId() + ">";
    }
}
