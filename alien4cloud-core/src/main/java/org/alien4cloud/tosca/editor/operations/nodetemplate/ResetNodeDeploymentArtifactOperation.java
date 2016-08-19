package org.alien4cloud.tosca.editor.operations.nodetemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Reset the deployment artifact of a node template to the configuration of the node type.
 */
@Getter
@Setter
public class ResetNodeDeploymentArtifactOperation extends AbstractNodeOperation {
    private String artifactName;

    @Override
    public String commitMessage() {
        return "reset the deployment artifact <" + artifactName + "> of node <" + getNodeName() + "> to it's original value.";
    }
}