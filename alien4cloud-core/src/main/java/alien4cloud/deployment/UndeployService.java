package alien4cloud.deployment;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentSetup;
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
    DeploymentSetupService deploymentSetupService;

    /**
     * Un-deploy a deployment object
     *
     * @param deploymentId deployment id to deploy
     */
    public synchronized void undeploy(String deploymentId) {
        Deployment deployment = deploymentService.getOrfail(deploymentId);
        undeploy(deployment);
    }

    /**
     * Un-deploy from a deployment setup.
     *
     * @param deploymentSetup setup object containing information to deploy
     */
    public synchronized void undeploy(DeploymentSetup deploymentSetup) {
        ApplicationEnvironment environment = deploymentSetupService.getApplicationEnvironment(deploymentSetup.getId());
        Deployment activeDeployment = deploymentService.getActiveDeploymentOrFail(environment.getId());
        undeploy(activeDeployment);
    }

    private void undeploy(Deployment deployment) {
        log.info("Un-deploying deployment [{}] on cloud [{}]", deployment.getId(), deployment.getOrchestratorId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.get(deployment.getOrchestratorId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment);
        orchestratorPlugin.undeploy(deploymentContext, null);
        alienDao.save(deployment);
        log.info("Un-deployed deployment [{}] on cloud [{}]", deployment.getId(), deployment.getOrchestratorId());
    }
}
