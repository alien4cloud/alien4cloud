package org.alien4cloud.tosca.editor.operations.nodetemplate;

/**
 * Allows to affect a get_input function to the property of a node.
 */
public class SetNodeCapabilityPropertyAsInputOperation extends SetNodePropertyAsInputOperation {
    /** Id of the capability */
    private String capabilityName;
}