package alien4cloud.model.components;

/**
 * Shared interface for Implementation and Deployment artifact.
 */
public interface IArtifact {
    String getArtifactType();

    String getArtifactRef();

    void setArtifactRef(String artifactRef);

    String getArchiveName();

    String getArtifactRepository();
}
