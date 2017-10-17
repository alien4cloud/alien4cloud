package org.alien4cloud.tosca.editor.operations.nodetemplate;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to delete a node from the topology.
 */
@Getter
@Setter
public class DeleteNodeOperation extends AbstractNodeOperation {
    /** Nodes artifacts to cleanup before commit. */
    private Map<String, DeploymentArtifact> artifacts;

    @Override
    public String commitMessage() {
        return "delete node <" + getNodeName() + ">";
    }
}
