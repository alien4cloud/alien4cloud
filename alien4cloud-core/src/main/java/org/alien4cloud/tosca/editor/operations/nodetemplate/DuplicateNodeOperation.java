package org.alien4cloud.tosca.editor.operations.nodetemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to duplicate a node template. <br>
 * If the node is a host, then copy along with it hostedOn hierarchy.<br>
 * Discard any relationship targeting a node out of the copied hostedOn hierarchy.
 *
 */
@Getter
@Setter
public class DuplicateNodeOperation extends AbstractNodeOperation {
    @Override
    public String commitMessage() {
        return "duplicate node <" + getNodeName() + "> with his hostedOn hierarchy. ";
    }
}
