package org.alien4cloud.tosca.editor.operations.nodetemplate.secrets;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to remove get_input function to the property of a node.
 */
@Getter
@Setter
public class UnsetNodeCapabilityPropertyAsSecretOperation extends UnsetNodePropertyAsSecretOperation {
    /** Id of the capability */
    private String capabilityName;
}