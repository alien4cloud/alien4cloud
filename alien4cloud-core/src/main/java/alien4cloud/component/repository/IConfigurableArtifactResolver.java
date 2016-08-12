package alien4cloud.component.repository;

import java.nio.file.Path;

public interface IConfigurableArtifactResolver<T> {

    boolean canHandleArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials);

    Path resolveArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials);

    T getConfiguration();

    void setConfiguration(T configuration);

}
