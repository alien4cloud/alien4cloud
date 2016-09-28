package alien4cloud.orchestrators.services;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;

/**
 * Helper service to get deployment properties definitions for an orchestrator.
 */
@Service
public class OrchestratorDeploymentService {
    @Inject
    private OrchestratorService orchestratorService;

    public Map<String, PropertyDefinition> getDeploymentPropertyDefinitions(String orchestratorId) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(orchestrator);
        return orchestratorFactory.getDeploymentPropertyDefinitions();
    }
}
