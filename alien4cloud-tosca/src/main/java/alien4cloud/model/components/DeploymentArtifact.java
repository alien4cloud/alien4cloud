package alien4cloud.model.components;

import lombok.Setter;

public class DeploymentArtifact extends AbstractArtifact {
    /** Specifies the display name of the artifact. */
    @Setter
    private String artifactName;

    public String getArtifactName() {
        return artifactName != null ? artifactName : getArtifactRef();
    }
}