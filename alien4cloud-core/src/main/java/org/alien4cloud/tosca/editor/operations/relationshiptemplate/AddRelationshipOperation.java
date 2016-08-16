package org.alien4cloud.tosca.editor.operations.relationshiptemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 */
@Getter
@Setter
public class AddRelationshipOperation extends AbstractRelationshipOperation {
    /** Name of the relationship type. */
    private String relationshipType;
    /** Version of the archive that contains the relationship type. */
    private String relationshipVersion;

    /** Name of the requirement on the source node. */
    private String requirementName;

    /** Name of the node template that is target of the relationship. */
    private String target;
    /** Name of the capability on the target node. */
    private String targetedCapabilityName;

    @Override
    public String commitMessage() {
        return "add relationship <" + getRelationshipName() + "> of type <" + relationshipType + ":" + relationshipVersion + "> source: [ node <"
                + getNodeName() + "> requirement <" + requirementName + "> ] target: [ node <" + target + "> capability <" + targetedCapabilityName + "> ]";
    }
}
