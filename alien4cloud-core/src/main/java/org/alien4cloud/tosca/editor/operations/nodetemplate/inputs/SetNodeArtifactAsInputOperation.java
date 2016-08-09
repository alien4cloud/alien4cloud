package org.alien4cloud.tosca.editor.operations.nodetemplate.inputs;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Allows to affect a get_input function to the property of a node.
 */
public class SetNodeArtifactAsInputOperation extends AbstractNodeOperation {
    private String inputName;
    private String artifactName;

    @Override
    public String commitMessage() {
        return "set the artifact <" + inputName + "> of node <" + getNodeName() + "> to input artifact <" + artifactName + ">";
    }
}