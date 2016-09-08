package alien4cloud.component.repository;

import java.nio.file.Path;
import java.util.Map;

import alien4cloud.repository.model.ValidationResult;

public interface IArtifactResolver {

    String getResolverType();

    ValidationResult canHandleArtifact(String artifactReference, String repositoryURL, String repositoryType, Map<String, Object> credentials);

    Path resolveArtifact(String artifactReference, String repositoryURL, String repositoryType, Map<String, Object> credentials);
}
