package alien4cloud.deployment;

import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.model.PaaSDeploymentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;

/**
 * Manages topology workflows.
 */
@Service
@Slf4j
public class WorkflowExecutionService {
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;

    /**
     * Launch a given workflow.
     */
    public synchronized void launchWorkflow(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, String applicationEnvironmentId, String workflowName, Map<String, Object> params, IPaaSCallback<?> iPaaSCallback) {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology, secretProviderConfigurationAndCredentials);
        orchestratorPlugin.launchWorkflow(deploymentContext, workflowName, params, iPaaSCallback);
    }

}
