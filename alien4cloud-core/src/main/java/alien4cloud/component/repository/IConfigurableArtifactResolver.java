package alien4cloud.component.repository;

public interface IConfigurableArtifactResolver<T> extends IArtifactResolver {

    void setConfiguration(T configuration);

    Class<T> getResolverConfigurationType();
}
