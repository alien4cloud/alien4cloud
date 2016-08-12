package alien4cloud.component.repository;

import java.io.InputStream;
import java.nio.file.Path;

public interface IArtifactResolver {

    String getResolverType();

    boolean canHandleArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials);

    Path resolveArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials);
}
