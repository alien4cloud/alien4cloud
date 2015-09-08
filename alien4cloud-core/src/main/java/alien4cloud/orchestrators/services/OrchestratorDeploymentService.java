package alien4cloud.orchestrators.services;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.paas.IDeploymentParameterizablePaaSProviderFactory;

/**
 * Helper service to get deployment properties definitions for an orchestrator.
 */
@Service
public class OrchestratorDeploymentService {
    @Inject
    private OrchestratorService orchestratorService;
    @Inject
    private OrchestratorFactoriesRegistry orchestratorFactoriesRegistry;

    public Map<String, PropertyDefinition> getDeploymentPropertyDefinitions(String orchestratorId) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        IOrchestratorPluginFactory orchestratorFactory = orchestratorFactoriesRegistry.getPluginBean(orchestrator.getPluginId(), orchestrator.getPluginBean());
        if (orchestratorFactory instanceof IDeploymentParameterizablePaaSProviderFactory) {
            return ((IDeploymentParameterizablePaaSProviderFactory) orchestratorFactory).getDeploymentPropertyDefinitions();
        }
        return null;
    }
}
