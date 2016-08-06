package org.alien4cloud.tosca.editor.operations.nodetemplate.inputs;

/**
 * Allows to remove get_input function to the property of a node.
 */
public class UnsetNodeCapabilityPropertyAsInputOperation extends UnsetNodePropertyAsInputOperation {
    /** Id of the capability */
    private String capabilityName;
}