package alien4cloud.component.repository;

import java.util.Map;

import alien4cloud.repository.model.ValidationResult;

/**
 * An artifact resolver determines if an artifact can be resolved for validation purpose, it can also download the artifact then return back to Alien for the
 * deployment of the topology
 */
public interface IArtifactResolver {

    /**
     * The type of the resolver, this is useful to distinguish different types of resolver for example git, http, maven, make sure to not having multiple
     * resolver plugins with the same resolver type
     * 
     * @return the type of the resolver
     */
    String getResolverType();

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
}
