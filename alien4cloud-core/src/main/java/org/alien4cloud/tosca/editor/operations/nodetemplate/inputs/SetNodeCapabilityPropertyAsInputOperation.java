package org.alien4cloud.tosca.editor.operations.nodetemplate.inputs;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to affect a get_input function to the property of a node.
 */
@Getter
@Setter
public class SetNodeCapabilityPropertyAsInputOperation extends SetNodePropertyAsInputOperation {
    /** Id of the capability */
    private String capabilityName;
}