package alien4cloud.orchestrators.services;

import java.io.IOException;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.utils.ReflectionUtil;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.rest.utils.JsonUtil;

/**
 * Manages orchestrator configuration
 */
@Service
public class OrchestratorConfigurationService {
    @Inject
    private OrchestratorService orchestratorService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;

    /**
     * Return the type of configuration for a given orchestrator.
     *
     * @param id Id of the orchestrator for which to get configuration.
     * @return The type of the orchestrator.
     */
    public Class<?> getConfigurationType(String id) {
        Orchestrator orchestrator = orchestratorService.getOrFail(id);
        return getConfigurationType(orchestrator);
    }

    /**
     * Return the type of configuration for a given orchestrator.
     *
     * @param orchestrator Orchestrator for which to get configuration.
     * @return The type of the orchestrator.
     */
    private Class<?> getConfigurationType(Orchestrator orchestrator) {
        IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestrator);
        return orchestratorFactory.getConfigurationType();
    }

    /**
     * Ensure that the configuration object parsed from json without typing is valid based on the orchestrator configuration type and return a valid typed
     * object.
     *
     * @param id if of the orchestrator.
     * @param configurationAsMap Configuration object (that may be a map parsed from json).
     * @return A typed configuration object.
     */
    public Object configurationAsValidObject(String id, Object configurationAsMap) throws IOException, PluginConfigurationException {
        Orchestrator orchestrator = orchestratorService.getOrFail(id);
        return configurationAsValidObject(orchestrator, configurationAsMap);
    }

    /**
     * Get the configuration for a given orchestrator.
     *
     * @param id Id of the orchestrator for which to get the configuration.
     * @return the instance of configuration for the given orchestrator
     */
    public OrchestratorConfiguration getConfigurationOrFail(String id) {
        OrchestratorConfiguration configuration = alienDAO.findById(OrchestratorConfiguration.class, id);
        if (configuration == null) {
            throw new NotFoundException("Orchestrator Configuration for id [" + id + "] doesn't exists.");
        }
        return configuration;
    }

    /**
     * Ensure that the configuration object parsed from json without typing is valid based on the orchestrator configuration type and return a valid typed
     * object.
     *
     * @param orchestrator Orchestrator for which to validated and compute a type configuration object.
     * @param configurationAsMap Configuration object (that may be a map parsed from json).
     * @return A typed configuration object.
     */
    private Object configurationAsValidObject(Orchestrator orchestrator, Object configurationAsMap) throws PluginConfigurationException, IOException {
        Class<?> configurationType = getConfigurationType(orchestrator);
        if (configurationType == null) {
            String message = "Orchestrator <" + orchestrator.getId() + "> using plugin <" + orchestrator.getPluginId() + "> <" + orchestrator.getPluginBean()
                    + "> cannot have configuration set (plugin has no configuration type).";
            throw new PluginConfigurationException(message);
        }

        return JsonUtil.readObject(JsonUtil.toString(configurationAsMap), configurationType);
    }

    /**
     * Update the configuration for the given cloud.
     *
     * @param id Id of the orchestrator for which to update the configuration.
     * @param newConfiguration The new configuration.
     */
    public synchronized void updateConfiguration(String id, Object newConfiguration) throws PluginConfigurationException, IOException {
        OrchestratorConfiguration configuration = alienDAO.findById(OrchestratorConfiguration.class, id);
        if (configuration == null) {
            throw new NotFoundException("No configuration exists for cloud [" + id + "].");
        }

        Object oldConfiguration = configuration.getConfiguration();
        Object oldConfigurationObj = configurationAsValidObject(id, oldConfiguration);
        // merge the config so that old values are preserved
        ReflectionUtil.mergeObject(newConfiguration, oldConfigurationObj);
        configuration.setConfiguration(oldConfigurationObj);

        // Trigger update of the orchestrator's configuration if enabled.
        IOrchestratorPlugin orchestratorInstance = (IOrchestratorPlugin) orchestratorPluginService.get(id);
        if (orchestratorInstance != null) {
            orchestratorInstance.setConfiguration(oldConfigurationObj);
        }

        alienDAO.save(configuration);
    }
}
