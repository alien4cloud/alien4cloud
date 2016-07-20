package org.alien4cloud.tosca.editor.operations.nodetemplate;

/**
 * Allows to affect a get_input function to the property of a node.
 */
public class SetNodeArtifactAsInputOperation extends AbstractNodeOperation {
    private String artifactName;
    private String inputName;

    @Override
    public String commitMessage() {
        return "set the artifact <" + inputName + "> of node <" + getNodeName() + "> to input artifact <" + artifactName + ">";
    }
}