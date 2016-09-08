package org.alien4cloud.tosca.editor.operations.inputs;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Created by lucboutier on 07/09/16.
 */
public class AddInputArtifactOperation extends AbstractNodeOperation {
    private String inputName;
    private String artifactName;

    @Override
    public String commitMessage() {
        return "add artifact ";
    }
}
