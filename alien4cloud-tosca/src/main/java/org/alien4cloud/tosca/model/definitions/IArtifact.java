package org.alien4cloud.tosca.model.definitions;

import java.util.Map;

/**
 * Shared interface for Implementation and Deployment artifact.
 */
public interface IArtifact {
    String getArtifactType();

    String getArtifactRef();

    void setArtifactRef(String artifactRef);

    String getArchiveName();

    String getArtifactRepository();

    /**
     * The path of artifact should be resolved just before the deployment by Alien for orchestrator plugins
     * 
     * @return local path to the artifact
     */
    String getArtifactPath();

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
    Map<String, Object> getRepositoryCredential();
}
