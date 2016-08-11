package alien4cloud.model.components;

import java.nio.file.Path;

public interface IArtifact {

    String getArtifactType();

    String getArtifactRef();

    String getArchiveName();

    String getArtifactRepository();

    /**
     * The path of artifact should be resolved just before the deployment by Alien for orchestrator plugins
     * 
     * @return local path to the artifact
     */
    Path getArtifactPath();

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
