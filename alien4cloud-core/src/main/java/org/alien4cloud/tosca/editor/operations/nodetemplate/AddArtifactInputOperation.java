package org.alien4cloud.tosca.editor.operations.nodetemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation to place an artifact as input for the topology.
 */
@Getter
@Setter
public class AddArtifactInputOperation extends AbstractNodeOperation {
    private String inputName;
    private String artifactName;
}