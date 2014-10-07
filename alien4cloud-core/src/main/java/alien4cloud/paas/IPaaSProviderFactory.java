package alien4cloud.paas;

/**
 * Allows creation of IPaaSProviders instances.
 */
public interface IPaaSProviderFactory {
    /** Create a new IPaaSProvider instance. */
    IPaaSProvider newInstance();
}