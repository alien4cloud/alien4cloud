package org.alien4cloud.tosca.model.definitions;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class DeploymentArtifact extends AbstractArtifact {
    /** Specifies the display name of the artifact. */
    private String artifactName;

    /** The path where the artifact must be copied to the target host. */
    @Getter
    private String deployPath;
    @Getter
    private String description;

    public String getArtifactName() {
        return artifactName != null ? artifactName : getArtifactRef();
    }

    /**
     * Constructor is used to create an artifact out of the artifact reference. This is used when parsing short notation.
     *
     * @param artifactRef The reference of the artifact within the archive.
     */
    public DeploymentArtifact(String artifactRef) {
        super(artifactRef);
    }

    @Override
    public String toString() {
        return "DeploymentArtifact{" + "artifactName='" + artifactName + '\'' + "} " + super.toString();
    }
}