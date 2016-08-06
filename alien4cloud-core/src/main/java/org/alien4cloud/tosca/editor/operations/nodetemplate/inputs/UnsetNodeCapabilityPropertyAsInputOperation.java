package org.alien4cloud.tosca.editor.operations.nodetemplate.inputs;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to remove get_input function to the property of a node.
 */
@Getter
@Setter
public class UnsetNodeCapabilityPropertyAsInputOperation extends UnsetNodePropertyAsInputOperation {
    /** Id of the capability */
    private String capabilityName;
}