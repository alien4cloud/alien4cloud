package alien4cloud.deployment;

import alien4cloud.dao.FilterUtil;
import alien4cloud.dao.IESQueryBuilderHelper;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.deployment.exceptions.ImpossibleDeploymentUpdateException;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.utils.MapUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

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
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private DeploymentContextService deploymentContextService;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;

    /**
     * Get an array of all active deployments.
     * 
     * @return Array of all active deployments.
     */
    public Deployment[] getActiveDeployments() {
        return alienDao.buildQuery(Deployment.class).prepareSearch().setFilters(fromKeyValueCouples("endDate", null)).search(0, Integer.MAX_VALUE).getData();
    }

    /**
     * Get all deployments for a given orchestrator an application
     *
     * @param orchestratorId Id of the cloud for which to get deployments (can be null to get deployments for all clouds).
     * @param sourceId Id of the application for which to get deployments (can be null to get deployments for all applications).
     * @param environmentId Id of the environment for which to get deployments (can be null to get deployments for all environments).
     * @return An array of deployments.
     */
    public Deployment[] getDeployments(String orchestratorId, String sourceId, String environmentId, int from, int size) {
        QueryBuilder filterBuilder = buildDeploymentFilters(orchestratorId, sourceId, environmentId);
        IESQueryBuilderHelper<Deployment> queryBuilderHelper = alienDao.buildQuery(Deployment.class);
        return queryBuilderHelper.setFilters(filterBuilder).prepareSearch().setFieldSort("startDate", true).search(from, size).getData();
    }

    /**
     * Search all deployments. See below to known with filters are supported.
     *
     * @param query Query text.
     * @param orchestratorId Id of the orchestrator for which to get deployments (can be null to get deployments for all orchestrator).
     * @param environmentId  Id of the environment for which to get deployments (can be null to get deployments for all environments).
     * @param sourceId Id of the application for which to get deployments (can be null to get deployments for all applications).
     * @param from Query from the given index.
     * @param size Maximum number of results to retrieve.
     * @return the deployments with pagination
     */
    public FacetedSearchResult searchDeployments(String query, String orchestratorId, String environmentId, String sourceId, int from, int size) {
        QueryBuilder filterBuilder = buildDeploymentFilters(orchestratorId, sourceId, environmentId);
        return alienDao.facetedSearch(Deployment.class, query, null, filterBuilder, null, from, size, "startDate", true);
    }

    private QueryBuilder buildDeploymentFilters(String orchestratorId, String sourceId, String environmentId) {
        QueryBuilder filterBuilder = null;
        if (orchestratorId != null) {
            QueryBuilder orchestratorFilter = QueryBuilders.termQuery("orchestratorId", orchestratorId);
            filterBuilder = filterBuilder == null ? orchestratorFilter : QueryBuilders.andQuery(orchestratorFilter, filterBuilder);
        }
        if (environmentId != null) {
            QueryBuilder environmentFilter = QueryBuilders.termQuery("environmentId", environmentId);
            filterBuilder = filterBuilder == null ? environmentFilter : QueryBuilders.andQuery(environmentFilter, filterBuilder);
        }
        if (sourceId != null) {
            QueryBuilder sourceFilter = QueryBuilders.termQuery("sourceId", sourceId);
            filterBuilder = filterBuilder == null ? sourceFilter : QueryBuilders.andQuery(sourceFilter, filterBuilder);
        }
        return filterBuilder;
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
     * Get a deployment for a given environment/
     *
     * @param applicationEnvironmentId id of the environment
     * @return active deployment if exist or the last, null if the application environment has not been deployed
     */
    public Deployment getDeployment(String applicationEnvironmentId) {
        Map<String, String[]> activeDeploymentFilters = MapUtil.newHashMap(new String[] { "environmentId" },
                new String[][] { new String[] { applicationEnvironmentId } });
        GetMultipleDataResult<Deployment> dataResult = alienDao.search(Deployment.class, null, activeDeploymentFilters, null, null, 0, Integer.MAX_VALUE,
                "endDate", true);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            if (dataResult.getData()[dataResult.getData().length - 1].getEndDate() == null) {
                return dataResult.getData()[dataResult.getData().length - 1];
            } else {
                return dataResult.getData()[0];
            }
        }
        return null;
    }

    /**
     * Get an active deployment for a given environment
     *
     * @param applicationEnvironmentId id of the environment
     * @return active deployment or null if not exist
     */
    public Deployment getActiveDeployment(String applicationEnvironmentId) {
        Map<String, String[]> activeDeploymentFilters = MapUtil.newHashMap(new String[] { "environmentId", "endDate" },
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
        GetMultipleDataResult<Deployment> dataResult = alienDao.find(Deployment.class, activeDeploymentFilters, 1);
        return dataResult.getData() != null && dataResult.getData().length > 0;
    }

    public Map<String, PaaSTopologyDeploymentContext> getCloudActiveDeploymentContexts(String orchestratorId) {
        Deployment[] deployments = getOrchestratorActiveDeployments(orchestratorId);
        Map<String, PaaSTopologyDeploymentContext> activeDeploymentContexts = Maps.newHashMap();
        for (Deployment deployment : deployments) {
            DeploymentTopology topology = deploymentRuntimeStateService.getRuntimeTopology(deployment.getId());
            activeDeploymentContexts.put(deployment.getOrchestratorDeploymentId(),
                    deploymentContextService.buildTopologyDeploymentContext(null, deployment, deploymentTopologyService.getLocations(topology), topology));
        }
        return activeDeploymentContexts;
    }

    private Deployment[] getOrchestratorActiveDeployments(String orchestratorId) {
        Map<String, String[]> activeDeploymentFilters = MapUtil.newHashMap(new String[] { "orchestratorId", "endDate" },
                new String[][] { new String[] { orchestratorId }, new String[] { null } });
        GetMultipleDataResult<Deployment> dataResult = alienDao.search(Deployment.class, null, activeDeploymentFilters, Integer.MAX_VALUE);
        return dataResult.getData();
    }

    /**
     * For a given environment get all deployments that have been and compute a map of deployment orchestrator ids by orchestrator id.
     *
     * @param applicationEnvironmentId The id of the application environment for which to get the map or pas deployment
     * @return A map of orchestrator deployment ids by orchestrator ids.
     */
    public Map<String, Set<String>> getOrchestratorDeploymentIdsByOrchestratorId(String applicationEnvironmentId) {
        Map<String, Set<String>> result = new HashMap<>();
        GetMultipleDataResult<Deployment> dataResult = alienDao.search(Deployment.class, null,
                FilterUtil.fromKeyValueCouples("environmentId", applicationEnvironmentId), Integer.MAX_VALUE);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            for (Deployment deployment : dataResult.getData()) {
                if (!result.containsKey(deployment.getOrchestratorId())) {
                    result.put(deployment.getOrchestratorId(), new HashSet<String>());
                }
                result.get(deployment.getOrchestratorId()).add(deployment.getOrchestratorDeploymentId());
            }
        }
        return result;
    }

    /**
     * Switch a deployment to undeployed.
     *
     * @param deployment the deployment to switch.
     */
    public void markUndeployed(Deployment deployment) {
        if (deployment.getEndDate() == null) {
            deployment.setEndDate(new Date());
            alienDao.save(deployment);
            // Switch the deployed field of the Deployment topology to false
            DeploymentTopology deploymentTopology = alienMonitorDao.findById(DeploymentTopology.class, deployment.getId());
            deploymentTopology.setDeployed(false);
            alienMonitorDao.save(deploymentTopology);
        } else {
            log.info("Deployment <" + deployment.getId() + "> is already marked as undeployed.");
        }
    }

    /**
     * Check if a CSAR is currently deployed through dependencies in a topology.
     *
     * @return True if the archive is used in a deployment, false if not.
     */
    public boolean isArchiveDeployed(String archiveName, String archiveVersion) {
        return alienMonitorDao.buildQuery(DeploymentTopology.class).prepareSearch()
                .setFilters(fromKeyValueCouples("deployed", "true", "dependencies.name", archiveName, "dependencies.version", archiveVersion)).count() > 0;
    }

    /**
     * Checks if a deployment can be updated using the provided {@link DeploymentTopology}
     *
     * @param deployment The deployment to update
     * @param deploymentTopology The deployment topology to use for the update
     *
     * @throws ImpossibleDeploymentUpdateException when the update is not possible
     */
    public void checkDeploymentUpdateFeasibility(Deployment deployment, DeploymentTopology deploymentTopology) {
        // for now just check if the locations are identical
        Set<String> deploymentLocations = Sets.newHashSet(deployment.getLocationIds());
        Collection<String> deploymentTopologyLocations = TopologyLocationUtils.getLocationIds(deploymentTopology).values();
        if (!CollectionUtils.isEqualCollection(deploymentLocations, deploymentTopologyLocations)) {
            throw new ImpossibleDeploymentUpdateException("Locations between the current deployment and the deployment topology do not match");
        }
    }
}