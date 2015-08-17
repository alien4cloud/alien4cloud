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
    /**
     * Create a new IOrchestrator instance.
     * 
     * @return An instance of the IOrchestrator.
     */
    T newInstance();

    /**
     * Can be called to destroy the context linked to this instance
     * 
     * @param instance provides the instance of IOrchestrator created by this factory that needs to be destroyed.
     */
    void destroy(T instance);

    /**
     * Get the default configuration for this provider.
     * 
     * @return Return an instance of the default configuration for the orchestrator.
     */
    V getDefaultConfiguration();

    /**
     * Get the type of the configuration.
     *
     * @return Type of the object that defines the cloud configuration.
     */
    Class<V> getConfigurationType();

    /**
     * Return a flag that indicates if the orchestrator is able to orchestrate mutliple locations.
     * 
     * @return true if the orchestrator created by this factory supports multiple locations, false if not.
     */
    boolean isMultipleLocations();
}