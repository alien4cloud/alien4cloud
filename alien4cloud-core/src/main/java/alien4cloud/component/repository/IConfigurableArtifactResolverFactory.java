package alien4cloud.component.repository;

/**
 * Factory to retrieve an {@link IConfigurableArtifactResolver}
 * 
 * @param <T>
 */
public interface IConfigurableArtifactResolverFactory<T> {

    /**
     * Create new instance of {@link IConfigurableArtifactResolver}
     * 
     * @return a newly created {@link IConfigurableArtifactResolver}
     */
    IConfigurableArtifactResolver<T> newInstance();

    /**
     * The resolver's configuration type
     * 
     * @return the type of the configuration
     */
    Class<T> getResolverConfigurationType();

    /**
     * Get the resolver type which the {@link IConfigurableArtifactResolver} can manage
     * 
     * @return the resolver's type
     */
    String getResolverType();
}
