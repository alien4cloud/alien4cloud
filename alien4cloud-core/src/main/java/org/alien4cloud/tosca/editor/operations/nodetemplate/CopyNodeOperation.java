package org.alien4cloud.tosca.editor.operations.nodetemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Copy a node template operation. <br>
 * If the node is a host, then copy along with it hostedOn hierarchy.<br>
 * Discard any relationship targeting a node out of the copied hostedOn hierarchy.
 *
 */
@Getter
@Setter
public class CopyNodeOperation extends AbstractNodeOperation {
    @Override
    public String commitMessage() {
        return "copy node <" + getNodeName() + "> with his hostedOn hierarchy. ";
    }
}
