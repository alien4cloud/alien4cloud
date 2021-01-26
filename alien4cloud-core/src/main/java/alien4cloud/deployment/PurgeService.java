package alien4cloud.deployment;

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
import org.alien4cloud.secret.services.SecretProviderService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;

/**
 * Manages topology purge.
 */
@Service
@Slf4j
public class PurgeService {
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private DeploymentLockService deploymentLockService;
    @Inject
    private SecretProviderService secretProviderService;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;
    /**
     * Purge a deployment object
     *
     * @param environmentId environment id to purge
     */
    public void purgeEnvironment(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, String environmentId) {
        Deployment deployment = deploymentService.getActiveDeployment(environmentId);
        if (deployment != null) {
            purge(secretProviderConfigurationAndCredentials, deployment);
        } else {
            log.warn("No deployment found for environment " + environmentId);
        }
    }

    /**
     * purge a deployment.
     *
     * @param deployment Depoyment to purge
     */
    private void purge(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, final Deployment deployment) {
        deploymentLockService.doWithDeploymentWriteLock(deployment.getOrchestratorDeploymentId(), () -> {
            log.info("Purging deployment [{}] on orchestrator [{}]", deployment.getId(), deployment.getOrchestratorId());
            IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
            DeploymentTopology deployedTopology = deploymentRuntimeStateService.getRuntimeTopology(deployment.getId());

            Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(deployedTopology);
            Map<String, Location> locations = deploymentTopologyService.getLocations(locationIds);

            SecretProviderConfigurationAndCredentials authResponse = null;
            if (secretProviderService.isSecretProvided(secretProviderConfigurationAndCredentials)) {
                authResponse = secretProviderService.generateToken(locations,
                        secretProviderConfigurationAndCredentials.getSecretProviderConfiguration().getPluginName(),
                        secretProviderConfigurationAndCredentials.getCredentials());
            }

            PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deployedTopology, authResponse);

            orchestratorPlugin.purge(deploymentContext, new IPaaSCallback<ResponseEntity>() {
                @Override
                public void onSuccess(ResponseEntity data) {
                    deploymentService.markUndeployed(deployment);
                    log.info("Deployment [{}] purged on orchestrator [{}]", deployment.getId(), deployment.getOrchestratorId());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.warn("Fail while purging deployment [{}] on orchestrator [{}]", deployment.getId(), deployment.getOrchestratorId());
                }
            });

            return null;
        });
    }
}
