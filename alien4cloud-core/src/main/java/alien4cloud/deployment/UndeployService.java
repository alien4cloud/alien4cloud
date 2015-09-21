package alien4cloud.deployment;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.model.PaaSDeploymentContext;

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
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;

    /**
     * Un-deploy a deployment object
     *
     * @param deploymentId deployment id to deploy
     */
    public synchronized void undeploy(String deploymentId) {
        Deployment deployment = deploymentService.getOrfail(deploymentId);
        undeploy(deployment);
    }

    public synchronized void undeployEnvironment(String environmentId) {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(environmentId);
        undeploy(deployment);
    }

    /**
     * Un-deploy from a deployment setup.
     *
     * @param deploymentTopology setup object containing information to deploy
     */
    public synchronized void undeploy(DeploymentTopology deploymentTopology) {
        Deployment activeDeployment = deploymentService.getActiveDeploymentOrFail(deploymentTopology.getEnvironmentId());
        undeploy(activeDeployment);
    }

    private void undeploy(Deployment deployment) {
        log.info("Un-deploying deployment [{}] on cloud [{}]", deployment.getId(), deployment.getOrchestratorId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        DeploymentTopology deployedTopology = deploymentRuntimeStateService.getRuntimeTopology(deployment.getId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deployedTopology);
        orchestratorPlugin.undeploy(deploymentContext, null);
        alienDao.save(deployment);
        log.info("Un-deployed deployment [{}] on cloud [{}]", deployment.getId(), deployment.getOrchestratorId());
    }
}
