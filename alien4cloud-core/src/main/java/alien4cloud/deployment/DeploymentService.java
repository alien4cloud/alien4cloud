package alien4cloud.deployment;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.stereotype.Service;

/**
 * Manage deployment operations on a cloud.
 */
@Service
@Slf4j
public class DeploymentService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Inject
    private DeploymentContextService deploymentContextService;

    /**
     * Get all deployments for a given cloud
     *
     * @param orchestratorId Id of the cloud for which to get deployments (can be null to get deployments for all clouds).
     * @param sourceId Id of the application for which to get deployments (can be null to get deployments for all applications).
     * @param from The start index of the query.
     * @param size The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} that contains deployments.
     */
    public GetMultipleDataResult<Deployment> getDeployments(String orchestratorId, String sourceId, int from, int size) {
        List<String> filterKeys = Lists.newArrayList();
        List<String[]> filterValues = Lists.newArrayList();
        if (orchestratorId != null) {
            filterKeys.add("orchestratorId");
            filterValues.add(new String[] { orchestratorId });
        }
        // if (locationId != null) {
        // filterKeys.add("locationId");
        // filterValues.add(new String[] { locationId });
        // }
        if (sourceId != null) {
            filterKeys.add("sourceId");
            filterValues.add(new String[] { sourceId });
        }
        // if (topologyId != null) {
        // filterKeys.add("topologyId");
        // filterValues.add(new String[] { topologyId });
        // }
        Map<String, String[]> filters = MapUtil.newHashMap(filterKeys.toArray(new String[filterKeys.size()]),
                filterValues.toArray(new String[filterValues.size()][]));

        return alienDao.search(Deployment.class, null, filters, from, size);
    }

    /**
     * Get a deployment given its id
     *
     * @param id id of the deployment
     * @return deployment with given id
     */
    public Deployment get(String id) {
        return alienDao.findById(Deployment.class, id);
    }

    /**
     * Get a deployment given its id
     *
     * @param id id of the deployment
     * @return deployment with given id
     */
    public Deployment getOrfail(String id) {
        Deployment deployment = alienDao.findById(Deployment.class, id);
        if (deployment == null) {
            throw new NotFoundException("Deployment <" + id + "> doesn't exist.");
        }
        return deployment;
    }

    /**
     * Get an active Deployment for a given environment or throw a NotFoundException if no active deployment can be found for this environment.
     * 
     * @param environmentId Id of the application environment.
     * @return The active deployment for this environment
     */
    public Deployment getActiveDeploymentOrFail(String environmentId) {
        Deployment deployment = getActiveDeployment(environmentId);
        if (deployment == null) {
            throw new NotFoundException("Deployment for environment <" + environmentId + "> doesn't exist.");
        }
        return deployment;
    }

    /**
     * Get an active deployment for a given environment
     *
     * @param applicationEnvironmentId id of the environment
     * @return active deployment or null if not exist
     */
    public Deployment getActiveDeployment(String applicationEnvironmentId) {
        Map<String, String[]> activeDeploymentFilters = MapUtil.newHashMap(new String[] { "deploymentSetup.environmentId", "endDate" },
                new String[][] { new String[] { applicationEnvironmentId }, new String[] { null } });
        GetMultipleDataResult<Deployment> dataResult = alienDao.search(Deployment.class, null, activeDeploymentFilters, 1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            return dataResult.getData()[0];
        }
        return null;
    }

    /**
     * Get an active Deployment for a given cloud and topology or throw a NotFoundException if no active deployment can be found.
     *
     * @param topologyId id of the topology that has been deployed
     * @param orchestratorId id of the target orchestrator.
     * @return a deployment
     * @throws alien4cloud.exception.NotFoundException if not any deployment exists
     */
    public Deployment getActiveDeploymentOrFail(String topologyId, String orchestratorId) {
        Deployment deployment = getActiveDeployment(orchestratorId, topologyId);
        if (deployment == null) {
            throw new NotFoundException("Deployment for cloud <" + orchestratorId + "> and topology <" + topologyId + "> doesn't exist.");
        }
        return deployment;
    }

    /**
     * Get a topology for a given cloud / topology
     *
     * @param orchestratorId targeted orchestrator id
     * @param topologyId id of the topology to deploy
     * @return a deployment
     */
    public Deployment getActiveDeployment(String orchestratorId, String topologyId) {
        Deployment deployment = null;
        Map<String, String[]> activeDeploymentFilters = MapUtil.newHashMap(new String[] { "orchestratorId", "topologyId", "endDate" },
                new String[][] { new String[] { orchestratorId }, new String[] { topologyId }, new String[] { null } });
        GetMultipleDataResult<Deployment> dataResult = alienDao.search(Deployment.class, null, activeDeploymentFilters, 1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            deployment = dataResult.getData()[0];
        }
        return deployment;
    }

    /**
     * Check if there is an active deployment on a given orchestrator with the given orchestrator deployment id.
     * 
     * @param orchestratorId The if of the orchestrator for which to check if there is a deployment with the given orchestratorDeploymentId.
     * @param orchestratorDeploymentId Unique if of the deployment for a given orchestrator
     * @return True if there is an active deployment for theses ids, false if not.
     */
    public boolean isActiveDeployment(String orchestratorId, String orchestratorDeploymentId) {
        Map<String, String[]> activeDeploymentFilters = MapUtil.newHashMap(new String[] { "orchestratorId", "orchestratorDeploymentId", "endDate" },
                new String[][] { new String[] { orchestratorId }, new String[] { orchestratorDeploymentId }, new String[] { null } });
        GetMultipleDataResult<Deployment> dataResult = alienDao.find(Deployment.class, activeDeploymentFilters, Integer.MAX_VALUE);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            return true;
        }
        return false;
    }

    public Map<String, PaaSTopologyDeploymentContext> getCloudActiveDeploymentContexts(String orchestratorId) {
        Deployment[] deployments = getOrchestratorActiveDeployments(orchestratorId);
        Map<String, PaaSTopologyDeploymentContext> activeDeploymentContexts = Maps.newHashMap();
        for (Deployment deployment : deployments) {
            Topology topology = alienMonitorDao.findById(Topology.class, deployment.getId());
            activeDeploymentContexts.put(deployment.getOrchestratorDeploymentId(),
                    deploymentContextService.buildTopologyDeploymentContext(deployment, topology));
        }
        return activeDeploymentContexts;
    }

    private Deployment[] getOrchestratorActiveDeployments(String orchestratorId) {
        Map<String, String[]> activeDeploymentFilters = MapUtil.newHashMap(new String[] { "orchestratorId", "endDate" },
                new String[][] { new String[] { orchestratorId }, new String[] { null } });
        GetMultipleDataResult<Deployment> dataResult = alienDao.search(Deployment.class, null, activeDeploymentFilters, 1);
        return dataResult.getData();
    }

    /**
     * Get all deployments for a given deployment setup id
     *
     * @param deploymentSetupId deployment setup's id
     * @return deployment which have the given deployment setup id
     */
    public GetMultipleDataResult<Deployment> getDeploymentsByDeploymentSetup(String deploymentSetupId) {
        return alienDao.find(Deployment.class, MapUtil.newHashMap(new String[] { "deploymentSetup.id" }, new String[][] { new String[] { deploymentSetupId } }),
                Integer.MAX_VALUE);
    }
}