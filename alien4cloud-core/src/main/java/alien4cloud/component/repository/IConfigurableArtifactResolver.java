package alien4cloud.component.repository;

import java.nio.file.Path;

import alien4cloud.repository.model.ValidationResult;

public interface IConfigurableArtifactResolver<T> {

    ValidationResult canHandleArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials);

    Path resolveArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials);

    T getConfiguration();

    void setConfiguration(T configuration);

}
