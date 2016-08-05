package alien4cloud.component.repository;

import java.io.InputStream;

public interface IArtifactResolver {

    String getResolverType();

    boolean canHandleArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials);

    InputStream resolveArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials);
}
