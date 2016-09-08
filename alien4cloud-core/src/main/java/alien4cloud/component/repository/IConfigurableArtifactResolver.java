package alien4cloud.component.repository;

import java.nio.file.Path;
import java.util.Map;

import alien4cloud.repository.model.ValidationResult;

public interface IConfigurableArtifactResolver<T> {

    ValidationResult canHandleArtifact(String artifactReference, String repositoryURL, String repositoryType, Map<String, Object> credentials);

    Path resolveArtifact(String artifactReference, String repositoryURL, String repositoryType, Map<String, Object> credentials);

    T getConfiguration();

    void setConfiguration(T configuration);

}
