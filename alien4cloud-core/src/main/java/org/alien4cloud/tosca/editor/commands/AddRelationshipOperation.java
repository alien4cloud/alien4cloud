package org.alien4cloud.tosca.editor.commands;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 */
@Getter
@Setter
public class AddRelationshipOperation extends AbstractEditorOperation {
    /** Name of the relationship. */
    private String relationshipName;
    /** Name of the relationship type. */
    private String relationshipType;
    /** Version of the archive that contains the relationship type. */
    private String relationshipVersion;
    /** Name of the node template that is source of the relationship. */
    private String source;
    /** Name of the requirement on the source node. */
    private String requirementName;
    /** Type of the requirement on the source node. */
    private String requirementType;

    /** Name of the node template that is target of the relationship. */
    private String target;
    /** Name of the capability on the target node. */
    private String targetedCapabilityName;
}
