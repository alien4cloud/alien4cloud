package alien4cloud.deployment;

import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.model.PaaSDeploymentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Manages topology un-deployment.
 */
@Service
@Slf4j
public class UndeployService {
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private DeploymentLockService deploymentLockService;

    /**
     * Un-deploy a deployment object
     *
     * @param deploymentId deployment id to deploy
     */
    public void undeploy(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, String deploymentId) {
        Deployment deployment = deploymentService.getOrfail(deploymentId);
        undeploy(secretProviderConfigurationAndCredentials, deployment);
    }

    public void undeployEnvironment(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, String environmentId) {
        Deployment deployment = deploymentService.getActiveDeployment(environmentId);
        if (deployment != null) {
            undeploy(secretProviderConfigurationAndCredentials, deployment);
        } else {
            log.warn("No deployment found for environment " + environmentId);
        }
    }

    /**
     * Un-deploy from a deployment setup.
     *
     * @param deploymentTopology setup object containing information to deploy
     */
    public void undeploy(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, DeploymentTopology deploymentTopology) {
        Deployment activeDeployment = deploymentService.getActiveDeploymentOrFail(deploymentTopology.getEnvironmentId());
        undeploy(secretProviderConfigurationAndCredentials, activeDeployment);
    }

    private void undeploy(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, final Deployment deployment) {
        deploymentLockService.doWithDeploymentWriteLock(deployment.getOrchestratorDeploymentId(), () -> {
            log.info("Un-deploying deployment [{}] on orchestrator [{}]", deployment.getId(), deployment.getOrchestratorId());
            IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
            DeploymentTopology deployedTopology = deploymentRuntimeStateService.getRuntimeTopology(deployment.getId());
            PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deployedTopology, secretProviderConfigurationAndCredentials);
            orchestratorPlugin.undeploy(deploymentContext, new IPaaSCallback<ResponseEntity>() {
                @Override
                public void onSuccess(ResponseEntity data) {
                    deploymentService.markUndeployed(deployment);
                    log.info("Un-deployed deployment [{}] on orchestrator [{}]", deployment.getId(), deployment.getOrchestratorId());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.warn("Fail while Undeploying deployment [{}] on orchestrator [{}]", deployment.getId(), deployment.getOrchestratorId());
                }
            });
            return null;
        });
    }
}
