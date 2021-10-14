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
import alien4cloud.model.runtime.Task;
import alien4cloud.model.runtime.WorkflowStepInstance;
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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.List;
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

    @Inject
    private WorkflowStepInstanceService workflowStepInstanceService;

    @Inject
    private TaskService taskService;

    public void resetExecutionStep(Execution execution,String stepId) {
        Deployment deployment = deploymentService.getOrfail(execution.getDeploymentId());

        int count = 0;
        int error = 0;

        // Ensure that the execution is in failed state
        if (!execution.getStatus().equals(ExecutionStatus.FAILED)) {
            throw new IllegalStateException(String.format("Execution %s must be in FAILED state to reset step",execution.getId()));
        }

        // Ensure that the deployment is still alive
        if (deployment.getEndDate() != null) {
            throw new IllegalStateException(String.format("Can't reset workflow step because the deployment is no longer alive"));
        }

        FacetedSearchResult<WorkflowStepInstance> stepsResult = workflowStepInstanceService.searchInstances("", execution.getId(), 0, Integer.MAX_VALUE);
        for (WorkflowStepInstance wsi : stepsResult.getData()) {
            if (wsi.getStepId().equals(stepId)) {
                count++;
                if (wsi.isHasFailedTasks()) {
                    error++;
                }
            }
        }

        if (count == 0) {
            throw new IllegalStateException(String.format("Can only reset done or failed workflow step"));
        }

        final boolean stepDone = (error != 0);

        deploymentLockService.doWithDeploymentWriteLock(deployment.getOrchestratorDeploymentId(), () -> {
            PaaSDeploymentContext deploymentContext = buildContext(deployment, null);

            IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());

            log.debug("Reseting execution {} step {}",execution.getId(),stepId);

            orchestratorPlugin.resetStep(deploymentContext, execution, stepId, stepDone, new IPaaSCallback<Object>() {
                @Override
                public void onSuccess(Object data) {
                    log.info("Workflow step reseted Execution={} step={}",execution.getId(),stepId);

                    adjustWorkflowStep(execution,stepId,stepDone);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.error("Workflow step cannot be reset",throwable);
                }
            });
            log.info("Reseting execution {} step {}",execution.getId(),stepId);

            return null;
        });
    }

    private void adjustWorkflowStep(Execution execution,String stepId,boolean stepDone) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        boolQueryBuilder.must(QueryBuilders.termQuery("deploymentId", execution.getDeploymentId()));
        boolQueryBuilder.must(QueryBuilders.termQuery("executionId", execution.getId()));
        boolQueryBuilder.must(QueryBuilders.termQuery("stepId", stepId));

        if (stepDone == true) {
            WorkflowStepInstance[] steps = alienDao.customFindAll(WorkflowStepInstance.class,boolQueryBuilder).toArray(new WorkflowStepInstance[0]);

            for (WorkflowStepInstance step : steps) {
                step.setHasFailedTasks(false);
            }

            alienDao.save(steps);
            // Modify all task as succeeded
            log.info("Step do Mark OK");
        } else {
            // Delete all step instance
            alienDao.delete(WorkflowStepInstance.class,boolQueryBuilder);
        }
    }

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
            PaaSDeploymentContext deploymentContext = buildContext(deployment,secretProviderConfigurationAndCredentials);

            IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());

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

    private PaaSDeploymentContext buildContext(Deployment deployment,SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials) {
        DeploymentTopology deployedTopology = deploymentRuntimeStateService.getRuntimeTopology(deployment.getId());

        Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(deployedTopology);
        Map<String, Location> locations = deploymentTopologyService.getLocations(locationIds);

        SecretProviderConfigurationAndCredentials authResponse = null;

        if (secretProviderService.isSecretProvided(secretProviderConfigurationAndCredentials)) {
            authResponse = secretProviderService.generateToken(locations,
                    secretProviderConfigurationAndCredentials.getSecretProviderConfiguration().getPluginName(),
                    secretProviderConfigurationAndCredentials.getCredentials()
            );
        }

        return new PaaSDeploymentContext(deployment, deployedTopology, authResponse);
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
