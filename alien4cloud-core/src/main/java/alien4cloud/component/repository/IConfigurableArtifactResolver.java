package alien4cloud.component.repository;

import java.util.Map;

import alien4cloud.repository.model.ValidationResult;

/**
 * A configurable resolver, in general it contains in its configuration information to connect to repository
 * 
 * @param <T> the type of configuration for this resolver
 */
public interface IConfigurableArtifactResolver<T> {

    /**
     * Check if the resolver can handle the particular artifact
     *
     * @param artifactReference reference of the artifact
     * @param repositoryURL url of the repository
     * @param repositoryType type of the repository, it corresponds to the resolver type, normally the plugin will resolve artifact only of type defined in
     *            getResolverType
     * @param credentials the credentials to connect to the repository
     * @return the validation result
     */
    ValidationResult canHandleArtifact(String artifactReference, String repositoryURL, String repositoryType, Map<String, Object> credentials);

    /**
     * Resolve an artifact, try to download the artifact if possible and return the path to the downloaded artifact
     *
     * @param artifactReference reference of the artifact
     * @param repositoryURL url of the repository
     * @param repositoryType type of the repository, it corresponds to the resolver type, normally the plugin will resolve artifact only of type defined in
     *            getResolverType
     * @param credentials the credentials to connect to the repository
     * @return the path to the downloaded artifact
     */
    String resolveArtifact(String artifactReference, String repositoryURL, String repositoryType, Map<String, Object> credentials);

    /**
     * Get the configuration of the artifact resolver
     * 
     * @return the configuration of the resolver
     */
    T getConfiguration();

    String getConfigurationUrl();

    /**
     * Set the configuration of the artifact resolver
     * 
     * @param configuration the configuration of the resolver
     */
    void setConfiguration(T configuration);
}
