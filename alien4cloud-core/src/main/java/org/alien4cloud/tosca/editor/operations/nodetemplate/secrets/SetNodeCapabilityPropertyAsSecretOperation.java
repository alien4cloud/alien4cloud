package org.alien4cloud.tosca.editor.operations.nodetemplate.secrets;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to affect a get_input function to the property of a node.
 */
@Getter
@Setter
public class SetNodeCapabilityPropertyAsSecretOperation extends SetNodePropertyAsSecretOperation {
    /** Id of the capability */
    private String capabilityName;
}