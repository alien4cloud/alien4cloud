package alien4cloud.component.repository;

import java.io.InputStream;

public interface IConfigurableArtifactResolver<T> {

    boolean canHandleArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials);

    InputStream resolveArtifact(String artifactReference, String repositoryURL, String repositoryType, String credentials);

    void setConfiguration(T configuration);

}
