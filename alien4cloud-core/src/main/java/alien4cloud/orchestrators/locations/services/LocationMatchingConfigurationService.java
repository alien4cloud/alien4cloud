package alien4cloud.orchestrators.locations.services;

import java.util.Map;

import javax.inject.Inject;

import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import org.springframework.stereotype.Service;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.OrchestratorPluginService;

/**
 * Allow to retrieve the matching configuration for a given location.
 */
@Service
public class LocationMatchingConfigurationService {
    @Inject
    private OrchestratorService orchestratorService;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;

    /**
     * Get the matching configuration for a given location.
     *
     * @param location The location for which to get the configuration.
     * @return A map nodetype, matching configuration to be used for matching.
     */
    public Map<String, MatchingConfiguration> getMatchingConfiguration(Location location) {
        IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestratorService.getOrFail(location.getOrchestratorId()));
        ILocationConfiguratorPlugin configuratorPlugin = orchestratorFactory.getConfigurator(location.getInfrastructureType());
        return configuratorPlugin.getMatchingConfigurations();
    }
}