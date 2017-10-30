package org.alien4cloud.tosca.editor.operations.nodetemplate;

import javax.validation.constraints.NotNull;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation.Point;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the position of a node on the alien4cloud editor canvas.
 */
@Getter
@Setter
public class UpdateNodePositionOperation extends AbstractNodeOperation {
    /** Position of the node on the canvas. */
    @NotNull
    private Point coords;

    @Override
    public String commitMessage() {
        return "update node <" + getNodeName() + "> position.";
    }
}