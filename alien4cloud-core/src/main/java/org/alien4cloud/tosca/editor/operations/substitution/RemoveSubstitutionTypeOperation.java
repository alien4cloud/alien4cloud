package org.alien4cloud.tosca.editor.operations.substitution;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

/**
 * Remove a substitution type.
 */
@Getter
@Setter
public class RemoveSubstitutionTypeOperation extends AbstractEditorOperation {
    @Override
    public String commitMessage() {
        return "Delete declaration of this topology as a substitution.";
    }
}
