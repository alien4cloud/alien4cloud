package alien4cloud.component.repository;

public interface IConfigurableArtifactResolverFactory<T> {

    IConfigurableArtifactResolver<T> newInstance();

    Class<T> getResolverConfigurationType();

    String getResolverType();
}
