package org.alien4cloud.tosca.model.definitions;

import lombok.Getter;
import lombok.Setter;

@Setter
public class DeploymentArtifact extends AbstractArtifact {
    /** Specifies the display name of the artifact. */
    private String artifactName;

    @Getter
    private String description;

    public String getArtifactName() {
        return artifactName != null ? artifactName : getArtifactRef();
    }

    @Override
    public String toString() {
        return "DeploymentArtifact{" + "artifactName='" + artifactName + '\'' + "} " + super.toString();
    }
}