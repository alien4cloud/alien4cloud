package org.alien4cloud.tosca.model.definitions;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractArtifact implements IArtifact {
    /** This attribute specifies the type of this artifact. */
    private String artifactType;
    /** Specifies the reference of the artifact. */
    private String artifactRef;
    /**
     * Non TOSCA compliant property, the artifactRepository indicate where the artifact is stored. It might be in the archive it-self (in this case this
     * property is null), in alien's internal artifact repository (alien) or nexus, git, svn ...
     */
    private String artifactRepository;
    /** The name of the archive in which the original artifact lies. */
    private String archiveName;
    /** The version of the archive in which the original artifact lies. */
    private String archiveVersion;

    private String repositoryURL;

    private Map<String, Object> repositoryCredential;

    private String repositoryName;

    /**
     * The local path to retrieve the artifact. Attention this is normally set before deployment so that the plugin knows where to get artifact.
     */
    @JsonIgnore
    private String artifactPath;

    @Override
    public String toString() {
        return "AbstractArtifact{" + "artifactType='" + artifactType + '\'' + ", artifactRef='" + artifactRef + '\'' + ", artifactRepository='"
                + artifactRepository + '\'' + ", archiveName='" + archiveName + '\'' + ", archiveVersion='" + archiveVersion + '\'' + ", repositoryURL='"
                + repositoryURL + '\'' + ", repositoryName='" + repositoryName + '\'' + ", artifactPath=" + artifactPath + '}';
    }
}
