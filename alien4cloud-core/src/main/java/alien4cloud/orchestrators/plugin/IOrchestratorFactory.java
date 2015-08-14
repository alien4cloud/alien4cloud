package alien4cloud.orchestrators.plugin;

/**
 * Implementation of these class are responsible for providing the common settings for an orchestrators and creating instances responsible for orchestrators
 * connexion.
 * This is the entry point of an orchestrators plugin.
 *
 * @param <T> Type of the orchestrator that this factory creates.
 * @param <V> Type of the configuration of the orchestrator that the factory creates.
 */
public interface IOrchestratorFactory<T extends IOrchestrator<V>, V> {
    /** Create a new IOrchestrator instance. */
    T newInstance();

    /** Can be called to destroy the context linked to this instance **/
    void destroy(T instance);

    /** Get the default configuration for this provider **/
    V getDefaultConfiguration();

    /** Get the type of the configuration **/
    Class<V> getConfigurationType();
}