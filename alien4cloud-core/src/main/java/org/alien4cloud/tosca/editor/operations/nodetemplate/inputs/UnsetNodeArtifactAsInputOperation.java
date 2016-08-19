package org.alien4cloud.tosca.editor.operations.nodetemplate.inputs;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Allows to affect a get_input function to the property of a node.
 */
public class UnsetNodeArtifactAsInputOperation extends AbstractNodeOperation {
    private String artifactName;

    @Override
    public String commitMessage() {
        return "artifact <" + artifactName + "> from node <" + getNodeName() + "> is not linked to an input artifact anymore.";
    }
}