package org.alien4cloud.tosca.editor.operations.nodetemplate.inputs;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;

/**
 * Operation to place an artifact as input for the topology.
 */
@Getter
@Setter
public class AddArtifactInputOperation extends AbstractNodeOperation {
    private String inputName;
    private String artifactName;

    @Override
    public String commitMessage() {
        return "set the artifact <" + artifactName + "> from node <" + getNodeName() + "> to the input <" + inputName + ">";
    }
}