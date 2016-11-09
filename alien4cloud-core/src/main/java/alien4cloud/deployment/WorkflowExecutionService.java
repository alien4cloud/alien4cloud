package alien4cloud.deployment;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.model.PaaSDeploymentContext;
import lombok.extern.slf4j.Slf4j;

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
    public synchronized void launchWorkflow(String applicationEnvironmentId, String workflowName, Map<String, Object> params, IPaaSCallback<?> iPaaSCallback) {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology);
        orchestratorPlugin.launchWorkflow(deploymentContext, workflowName, params, iPaaSCallback);
    }

}
