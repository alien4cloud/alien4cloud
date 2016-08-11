package alien4cloud.model.components;

public interface IArtifact {

    String getArtifactType();

    String getArtifactRef();

    String getArchiveName();

    String getArtifactRepository();

    /**
     * Name of the repository as defined in the YAML for the artifact
     * 
     * @return Name of the repository
     */
    String getRepositoryName();

    /**
     * URL of the repository as defined in the YAML for the artifact
     * 
     * @return URL of the repository
     */
    String getRepositoryURL();

    /**
     * Credentials of the repository as defined in the YAML for the artifact (this is bad practice but defined in Tosca)
     * 
     * @return Credentials of the repository
     */
    String getRepositoryCredentials();
}
