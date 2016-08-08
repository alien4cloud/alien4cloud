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
public class AddRequirementSubstitutionTypeOperation extends AbstractEditorOperation {

    @NotBlank
    private String nodeTemplateName;

    @NotBlank
    private String substitutionRequirementId;

    @NotBlank
    private String requirementId;

    @Override
    public String commitMessage() {
        return "add requirement type substitution for <" + requirementId + "> of node <" + nodeTemplateName + ">";
    }
}
