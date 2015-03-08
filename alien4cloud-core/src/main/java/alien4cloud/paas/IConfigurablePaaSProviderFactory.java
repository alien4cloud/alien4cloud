package alien4cloud.paas;

public interface IConfigurablePaaSProviderFactory<T> extends IPaaSProviderFactory<IConfigurablePaaSProvider<T>> {

    /** Get the type of the configuration **/
    Class<T> getConfigurationType();

    /** Get the default configuration for this provider **/
    T getDefaultConfiguration();
}
