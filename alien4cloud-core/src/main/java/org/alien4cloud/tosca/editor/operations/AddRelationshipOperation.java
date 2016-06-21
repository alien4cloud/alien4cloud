package org.alien4cloud.tosca.editor.operations;

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
    /** Type of the requirement on the source node. */
    private String requirementType;

    /** Name of the node template that is target of the relationship. */
    private String target;
    /** Name of the capability on the target node. */
    private String targetedCapabilityName;
}
