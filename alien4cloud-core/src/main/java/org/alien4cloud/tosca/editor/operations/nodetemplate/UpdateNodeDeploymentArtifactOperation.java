package org.alien4cloud.tosca.editor.operations.nodetemplate;

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
    private String repositoryUrl;
    private String repositoryName;
    private String archiveName;
    private String archiveVersion;

    @Override
    public String commitMessage() {
        return "the deployment artifact <" + artifactName + "> of node <" + getNodeName() + "> is now set to the reference <" + artifactReference
                + "> in repository <" + artifactRepository + ">";
    }
}