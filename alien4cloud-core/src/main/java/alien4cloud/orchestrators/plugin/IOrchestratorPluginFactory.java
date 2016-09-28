package alien4cloud.orchestrators.plugin;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.model.orchestrators.ArtifactSupport;
import alien4cloud.model.orchestrators.locations.LocationSupport;

/**
 * Implementation of these class are responsible for providing the common settings for an orchestrators and creating instances responsible for orchestrators
 * connexion.
 * This is the entry point of an orchestrators plugin.
 *
 * @param <T> Type of the orchestrator that this factory creates.
 * @param <V> Type of the configuration of the orchestrator that the factory creates.
 */
public interface IOrchestratorPluginFactory<T extends IOrchestratorPlugin<V>, V> {
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
     * Return an object that contains informations on location(s) supported by the orchestrator.
     *
     * @return An instance of LocationSupport that contains the details on what locations the orchestrator can support.
     */
    LocationSupport getLocationSupport();

    /**
     * Return an object that contains informations on artifact(s) supported by the orchestrator.
     *
     * @return An instance of ArtifactSupport that contains the details on what artifacts the orchestrator can support.
     */
    ArtifactSupport getArtifactSupport();

    /**
     * Get the deployment property definition
     *
     * @return A map containing property definitions
     */
    Map<String, PropertyDefinition> getDeploymentPropertyDefinitions();

    /**
     * Get the type of orchestrator this factory provides. ex: cloudify3
     * 
     * @return
     */
    String getType();

}