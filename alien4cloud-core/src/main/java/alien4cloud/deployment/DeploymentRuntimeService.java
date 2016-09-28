package alien4cloud.deployment;

import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import org.alien4cloud.tosca.model.templates.Capability;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.MaintenanceModeException;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.paas.model.OperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.tosca.normative.NormativeComputeConstants;

/**
 * Manages operations performed on a running deployment.
 */
@Slf4j
@Service
public class DeploymentRuntimeService {
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentContextService deploymentContextService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;

    /**
     * Trigger the execution of an operation on a node.
     *
     * @param request the operation's execution description ( see {@link alien4cloud.paas.model.OperationExecRequest})
     * @throws alien4cloud.paas.exception.OperationExecutionException runtime exception during an operation
     */
    public void triggerOperationExecution(OperationExecRequest request, IPaaSCallback<Map<String, String>> callback) throws OperationExecutionException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(request.getApplicationEnvironmentId());
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        orchestratorPlugin.executeOperation(deploymentContextService.buildTopologyDeploymentContext(deployment,
                deploymentTopologyService.getLocations(deploymentTopology), deploymentTopology), request, callback);
    }

    /**
     * Switch an instance in a deployment to maintenance mode. If so the orchestrator should not perform self healing operations for this instance.
     *
     * @param applicationEnvironmentId The id of the application environment.
     * @param nodeTemplateId           The id of the node template on which to enable maintenance mode.
     * @param instanceId               The id of the instance.
     * @param maintenanceModeOn        true if we should enable the maintenance mode, false if we should disable it.
     * @throws MaintenanceModeException In case the operation fails.
     */
    public void switchInstanceMaintenanceMode(String applicationEnvironmentId, String nodeTemplateId, String instanceId, boolean maintenanceModeOn)
            throws MaintenanceModeException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology);
        orchestratorPlugin.switchInstanceMaintenanceMode(deploymentContext, nodeTemplateId, instanceId, maintenanceModeOn);
    }

    /**
     * Switch all instances in a deployment to maintenance mode. If so the orchestrator should not perform self healing operations for this instance.
     *
     * @param applicationEnvironmentId The id of the application environment.
     * @param maintenanceModeOn        true if we should enable the maintenance mode, false if we should disable it.
     * @throws MaintenanceModeException In case the operation fails.
     */
    public void switchMaintenanceMode(String applicationEnvironmentId, boolean maintenanceModeOn) throws MaintenanceModeException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology);
        orchestratorPlugin.switchMaintenanceMode(deploymentContext, maintenanceModeOn);
    }

    /**
     * Scale up/down a node in a topology
     *
     * @param applicationEnvironmentId id of the targeted environment
     * @param nodeTemplateId           id of the compute node to scale up
     * @param instances                the number of instances to be added (if positive) or removed (if negative)
     */
    public void scale(String applicationEnvironmentId, final String nodeTemplateId, int instances, final IPaaSCallback<Object> callback)
            throws OrchestratorDisabledException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        final DeploymentTopology topology = alienMonitorDao.findById(DeploymentTopology.class, deployment.getId());
        final Capability capability = TopologyUtils.getScalableCapability(topology, nodeTemplateId, true);
        final int previousInitialInstances = TopologyUtils.getScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, capability);
        final int newInitialInstances = previousInitialInstances + instances;
        log.info("Scaling <{}> node from <{}> to <{}>. Updating runtime topology...", nodeTemplateId, previousInitialInstances, newInitialInstances);
        TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, newInitialInstances, capability);
        alienMonitorDao.save(topology);
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology);
        orchestratorPlugin.scale(deploymentContext, nodeTemplateId, instances, new IPaaSCallback() {
            @Override
            public void onFailure(Throwable throwable) {
                log.info("Failed to scale <{}> node from <{}> to <{}>. rolling back to {}...", nodeTemplateId, previousInitialInstances, newInitialInstances,
                        previousInitialInstances);
                TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, previousInitialInstances, capability);
                alienMonitorDao.save(topology);
                callback.onFailure(throwable);
            }

            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(data);
            }
        });
    }
}