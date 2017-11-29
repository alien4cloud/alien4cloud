package org.alien4cloud.tosca.editor.operations.secrets;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to remove get_secret function to the capability of a node.
 */
@Getter
@Setter
public class UnsetNodeCapabilityPropertyAsSecretOperation extends UnsetNodePropertyAsSecretOperation {
    /** Id of the capability */
    private String capabilityName;
}