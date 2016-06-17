package org.alien4cloud.tosca.editor.commands;

import lombok.Getter;
import lombok.Setter;

/**
 * Update the deployment artifact of a node.
 */
@Getter
@Setter
public class UpdateNodeDeploymentArtifactOperation extends AbstractNodeOperation {
    private String artifactName;
    private String artifactRepository;
    private String artifactReference;
}