package alien4cloud.deployment;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.runtime.Execution;
import alien4cloud.model.runtime.ExecutionStatus;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.secret.services.SecretProviderService;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Map;

/**
 * Manage execution operations on a cloud.
 */
@Service
@Slf4j
public class ExecutionService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;

    @Inject
    private DeploymentService deploymentService;

    @Inject
    private DeploymentLockService deploymentLockService;

    @Inject
    private OrchestratorPluginService orchestratorPluginService;

    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;

    @Inject
    private SecretProviderService secretProviderService;

    @Inject
    private DeploymentTopologyService deploymentTopologyService;

    public void resumeExecution(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, Execution execution) {
        Deployment deployment = deploymentService.getOrfail(execution.getDeploymentId());

        // Ensure that the execution is in failed state
        if (!execution.getStatus().equals(ExecutionStatus.FAILED)) {
            throw new IllegalStateException(String.format("Execution %s must be in FAILED state to be resumed",execution.getId()));
        }

        // Ensure that the deployment is still alive
        if (deployment.getEndDate() != null) {
            throw new IllegalStateException(String.format("Can't resume execution %s because its deployment is no longer alive",execution.getId()));
        }

        deploymentLockService.doWithDeploymentWriteLock(deployment.getOrchestratorDeploymentId(), () -> {
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

            log.info("Resuming execution {} of deployment [{}] on orchestrator [{}]", execution.getId(), deployment.getId(), deployment.getOrchestratorId());

            orchestratorPlugin.resume(deploymentContext, execution, new IPaaSCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                }

                @Override
                public void onFailure(Throwable throwable) {
                }
            });

            return null;
        });
    }

    /**
     * Get an execution by it's id.
     */
    public Execution getExecution(String executionId) {
        return alienDao.findById(Execution.class, executionId);
    }

    /**
     * Search executions. See below to known with filters are supported.
     *
     * @param query Query text.
     * @param deploymentId Id of the deployment for which to get executions.
     * @param from Query from the given index.
     * @param size Maximum number of results to retrieve.
     * @return the deployments with pagination
     */
    public FacetedSearchResult searchExecutions(String query, String deploymentId, int from, int size) {
        QueryBuilder filterBuilder = buildFilters(deploymentId);
        return alienDao.facetedSearch(Execution.class, query, null, filterBuilder, null, from, size, "startDate", "date", true);
    }

    /**
     * For a given deployment, get the last known execution.
     */
    public Execution getLastExecution(String deploymentId) {
        QueryBuilder filterBuilder = buildFilters(deploymentId);
        FacetedSearchResult<Execution> executions = alienDao.facetedSearch(Execution.class, "", null, filterBuilder, null, 0, 1, "startDate", "date", true);
        if (executions.getData() != null && executions.getData().length > 0) {
            return executions.getData()[0];
        } else {
            return null;
        }
    }

    private QueryBuilder buildFilters(String deploymentId) {
        QueryBuilder filterBuilder = null;
        if (deploymentId != null) {
            QueryBuilder filter = QueryBuilders.termQuery("deploymentId", deploymentId);
            //filterBuilder = filterBuilder == null ? filter : QueryBuilders.andQuery(filter, filterBuilder);
            filterBuilder = QueryBuilders.boolQuery().must(filter);
        }
        return filterBuilder;
    }

}
