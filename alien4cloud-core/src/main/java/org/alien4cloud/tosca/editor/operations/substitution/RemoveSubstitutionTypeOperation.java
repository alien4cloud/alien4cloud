package org.alien4cloud.tosca.editor.operations.substitution;

import lombok.Getter;
import lombok.Setter;

/**
 * Remove a substitution type.
 */
@Getter
@Setter
public class RemoveSubstitutionTypeOperation extends AbstractTopologyTemplateOperation {

    @Override
    public String commitMessage() {
        return "remove substitution type of topology <" + getTopologyId() + ">";
    }
}
