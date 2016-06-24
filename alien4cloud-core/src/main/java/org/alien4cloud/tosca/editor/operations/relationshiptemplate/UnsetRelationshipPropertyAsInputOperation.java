package org.alien4cloud.tosca.editor.operations.relationshiptemplate;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Allows to remove get_input function to the property of a node.
 */
public class UnsetRelationshipPropertyAsInputOperation extends AbstractRelationshipOperation {
    /** Id of the property */
    private String propertyName;
}