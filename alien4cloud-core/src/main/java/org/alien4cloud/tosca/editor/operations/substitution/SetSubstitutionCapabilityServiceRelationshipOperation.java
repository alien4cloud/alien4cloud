package org.alien4cloud.tosca.editor.operations.substitution;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to set a service relationship on a substitution capability/requirement.
 */
@Getter
@Setter
public class SetSubstitutionCapabilityServiceRelationshipOperation extends AbstractEditorOperation {
    private String substitutionCapabilityId;
    /** Name of the relationship type. */
    private String relationshipType;
    /** Version of the archive that contains the relationship type. */
    private String relationshipVersion;

    @Override
    public String commitMessage() {
        return "Set service relationship type for substitution capability <" + substitutionCapabilityId + "> to <" + relationshipType + ":"
                + relationshipVersion + ">";
    }
}