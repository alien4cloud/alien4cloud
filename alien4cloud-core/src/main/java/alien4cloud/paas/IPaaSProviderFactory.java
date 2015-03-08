package alien4cloud.paas;

/**
 * Allows creation of IPaaSProviders instances.
 */
public interface IPaaSProviderFactory<T extends IPaaSProvider> {

    /** Create a new IPaaSProvider instance. */
    T newInstance();

    /** Can be called to destroy the context linked to this instance **/
    void destroy(T instance);
}