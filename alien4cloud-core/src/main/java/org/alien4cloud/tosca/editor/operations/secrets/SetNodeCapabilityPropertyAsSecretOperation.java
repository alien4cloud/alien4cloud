package org.alien4cloud.tosca.editor.operations.secrets;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows to affect a get_secret function to the capability of a node.
 */
@Getter
@Setter
public class SetNodeCapabilityPropertyAsSecretOperation extends SetNodePropertyAsSecretOperation {
    /** Id of the capability */
    private String capabilityName;
}