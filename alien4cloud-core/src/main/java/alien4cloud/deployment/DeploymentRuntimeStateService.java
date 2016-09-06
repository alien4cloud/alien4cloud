package alien4cloud.deployment;

import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.elasticsearch.mapping.QueryHelper;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSInstancePersistentResourceMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStateMonitorEvent;
import alien4cloud.paas.model.PaaSMessageMonitorEvent;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Maps;

/**
 * Manage runtime operations on deployments.
 */
@Service
public class DeploymentRuntimeStateService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Inject
    private QueryHelper queryHelper;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentContextService deploymentContextService;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;

    /**
     * Get the deployed (runtime) topology of an application from the environment id
     *
     * @param applicationEnvironmentId id of the environment
     * @return the DeploymentTopology requested if found
     */
    public DeploymentTopology getRuntimeTopologyFromEnvironment(String applicationEnvironmentId) {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        return alienMonitorDao.findById(DeploymentTopology.class, deployment.getId());
    }

    /**
     * Get the deployed (runtime) topology of an application from the deployment's id
     * 
     * @param deploymentId
     * @return
     */
    public DeploymentTopology getRuntimeTopology(String deploymentId) {
        return alienMonitorDao.findById(DeploymentTopology.class, deploymentId);
    }

    /**
     * Get the deployed (runtime) topology of an application on a cloud
     *
     * @param topologyId id of the topology for which to get deployed topology.
     * @param orchestratorId targeted cloud id
     * @return the DeploymentTopology requested if found
     */
    public DeploymentTopology getRuntimeTopologyFromEnvironment(String topologyId, String orchestratorId) {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(topologyId, orchestratorId);
        return alienMonitorDao.findById(DeploymentTopology.class, deployment.getId());
    }

    /**
     * Get the current deployment status for a topology.
     *
     * @param deployment deployment for which we want the status
     * @param callback that will be called when status is available*
     * @return The status of the topology.
     * @throws alien4cloud.paas.exception.OrchestratorDisabledException In case the cloud selected for the topology is disabled.
     */
    public void getDeploymentStatus(final Deployment deployment, final IPaaSCallback<DeploymentStatus> callback) throws OrchestratorDisabledException {
        if (deployment == null) {
            callback.onSuccess(DeploymentStatus.UNDEPLOYED);
            return;
        }
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());

        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, getRuntimeTopology(deployment.getId()));
        IPaaSCallback<DeploymentStatus> esCallback = new IPaaSCallback<DeploymentStatus>() {
            @Override
            public void onSuccess(DeploymentStatus data) {
                if (data == DeploymentStatus.UNDEPLOYED) {
                    deploymentService.markUndeployed(deployment);
                }
                callback.onSuccess(data);
            }

            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }
        };
        orchestratorPlugin.getStatus(deploymentContext, esCallback);
    }

    /**
     * Get the detailed status for each instance of each node template.
     *
     * @param deployment The deployment for witch to get the instance informations.
     * @param callback callback on witch to send the map of node template's id to map of instance's id to instance information.
     * @throws alien4cloud.paas.exception.OrchestratorDisabledException In case the cloud selected for the topology is disabled.
     */
    public void getInstancesInformation(final Deployment deployment, IPaaSCallback<Map<String, Map<String, InstanceInformation>>> callback)
            throws OrchestratorDisabledException {
        Map<String, Map<String, InstanceInformation>> instancesInformation = Maps.newHashMap();
        if (deployment == null) {
            callback.onSuccess(instancesInformation);
            return;
        }
        DeploymentTopology runtimeTopology = alienMonitorDao.findById(DeploymentTopology.class, deployment.getId());
        PaaSTopologyDeploymentContext deploymentContext = deploymentContextService.buildTopologyDeploymentContext(deployment,
                deploymentTopologyService.getLocations(runtimeTopology), runtimeTopology);
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        orchestratorPlugin.getInstancesInformation(deploymentContext, callback);
    }

    /**
     * Get events for a specific deployment from an environment
     *
     * @param applicationEnvironmentId The environment we want to get events from
     * @param from The initial position of the events to get (based on time desc sorting)
     * @param size The number of events to get.
     * @return A result that contains all events.
     */
    public GetMultipleDataResult<?> getDeploymentEvents(String applicationEnvironmentId, int from, int size) {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        String index = alienMonitorDao.getIndexForType(AbstractMonitorEvent.class);
        QueryHelper.SearchQueryHelperBuilder searchQueryHelperBuilder = queryHelper.buildSearchQuery(index)
                .types(PaaSDeploymentStatusMonitorEvent.class, PaaSInstanceStateMonitorEvent.class, PaaSMessageMonitorEvent.class,
                        PaaSInstancePersistentResourceMonitorEvent.class)
                .filters(MapUtil.newHashMap(new String[] { "deploymentId" }, new String[][] { new String[] { deployment.getId() } }))
                .fieldSort("_timestamp", true);
        return alienMonitorDao.search(searchQueryHelperBuilder, from, size);
    }
}