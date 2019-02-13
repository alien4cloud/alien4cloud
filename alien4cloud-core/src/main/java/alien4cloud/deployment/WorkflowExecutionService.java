package alien4cloud.deployment;

import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.secret.services.SecretProviderService;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
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
    @Inject
    private SecretProviderService secretProviderService;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;

    /**
     * Launch a given workflow.
     */
    public synchronized void launchWorkflow(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials,
            String applicationEnvironmentId, String workflowName, Map<String, Object> params, IPaaSCallback<String> iPaaSCallback) {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        final DeploymentTopology topology = alienMonitorDao.findById(DeploymentTopology.class, deployment.getId());
        // get the secret provider configuration from the location
        Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(topology);
        Map<String, Location> locations = deploymentTopologyService.getLocations(locationIds);
        SecretProviderConfigurationAndCredentials authResponse = null;
        if (secretProviderService.isSecretProvided(secretProviderConfigurationAndCredentials)) {
            authResponse = secretProviderService.generateToken(locations,
                    secretProviderConfigurationAndCredentials.getSecretProviderConfiguration().getPluginName(),
                    secretProviderConfigurationAndCredentials.getCredentials());
        }
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology, authResponse);
        orchestratorPlugin.launchWorkflow(deploymentContext, workflowName, params, iPaaSCallback);
    }

}
