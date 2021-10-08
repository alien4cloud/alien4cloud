package alien4cloud.rest.deployment;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.deployment.*;
import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.runtime.Execution;
import alien4cloud.model.runtime.ExecutionStatus;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSWorkflowMonitorEvent;
import alien4cloud.rest.model.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping({ "/rest/executions", "/rest/v1/executions", "/rest/latest/executions" })
@Api(value = "", description = "Operations on Executions")
public class ExecutionController {

    private static final String TOPIC_PREFIX = "/topic/deployment-events";

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ExecutionService executionService;

    @Resource
    private DeploymentService deploymentService;

    @Resource
    private DeploymentRuntimeStateService deploymentRuntimeStateService;

    @Resource
    private OrchestratorPluginService orchestratorPluginService;

    @Resource
    private SimpMessagingTemplate template;

    /**
     * Search for executions
     *
     * @return A rest response that contains a {@link FacetedSearchResult} containing executions.
     */
    @ApiOperation(value = "Search for executions", notes = "Returns a search result with that contains executions matching the request.")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult> search(
            @ApiParam(value = "Query text.") @RequestParam(required = false) String query,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "50") int size,
            @ApiParam(value = "Id of the deployment for which to get executions. If not provided, get executions for all deployments") @RequestParam(required = false) String deploymentId
            ) {
        FacetedSearchResult searchResult = executionService.searchExecutions(query, deploymentId, from, size);
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

    /**
     * Search for one execution from its id
     *
     * @return A rest response that contains the execution if found.
     */
    @ApiOperation(value = "Search for an execution from its id", notes = "Returns the execution if found.")
    @RequestMapping(value ="/{id}", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Execution> search(@ApiParam(value = "Execution id.") @PathVariable String id) {
        Execution execution = alienDAO.findById(Execution.class, id);
        if (execution != null) {
           return RestResponseBuilder.<Execution> builder().data(execution).build();
        } else {
           return RestResponseBuilder.<Execution> builder().data(null)
                    .error(new RestError(RestErrorCode.NOT_FOUND_ERROR.getCode(), "Execution with id <" + id + "> was not found.")).build();
        }
    }

    @ApiOperation(value = "Resume a workflow execution", notes = "resume a workflow execution.")
    @RequestMapping(value = "/{executionId}/resume", method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Void> resumeWorkflowExecution(
            @ApiParam(value = "Execution id.", required = true) @Valid @NotBlank @PathVariable String executionId,
            @ApiParam(value = "The secret provider configuration and credentials.") @RequestBody SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials
    ) {

        // Find the execution
        Execution execution = executionService.getExecution(executionId);
        if (execution == null) {
            RestError error = RestErrorBuilder.builder(RestErrorCode.NOT_FOUND_ERROR).message("Execution not found").build();
            return RestResponseBuilder.<Void>builder().error(error).build();
        }

        executionService.resumeExecution(secretProviderConfigurationAndCredentials,execution);

        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Cancel an execution", notes = "Cancel a running execution.")
    @RequestMapping(value = "/cancel", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public DeferredResult<RestResponse<Void>> cancel(@RequestBody ExecutionCancellationRequest request) {
        final DeferredResult<RestResponse<Void>> result = new DeferredResult<>(15L * 60L * 1000L);

        // Building context
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(request.getEnvironmentId());
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        PaaSDeploymentContext context = new PaaSDeploymentContext(deployment,deploymentTopology,null);

        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());

        orchestratorPlugin.cancelTask(context, request.getExecutionId(), new IPaaSCallback<String>() {
            @Override
            public void onSuccess(String data) {
                cancellationRequested(request.getExecutionId());
                notifyEvent(context,request.getExecutionId());
                result.setResult(RestResponseBuilder.<Void> builder().build());
            }

            @Override
            public void onFailure(Throwable throwable) {
                result.setErrorResult(RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.NOT_FOUND_ERROR.getCode(), throwable.getMessage())).build());
            }
        });

        return result;
    }

    private void cancellationRequested(String executionId) {
        Execution execution = alienDAO.findById(Execution.class, executionId);
        if (execution != null) {
            execution.setStatus(ExecutionStatus.CANCELLING);
            alienDAO.save(execution);
        }
    }

    private void notifyEvent(PaaSDeploymentContext context,String executionId) {
        String topicName = TOPIC_PREFIX + '/' + context.getDeploymentId()+ "/paasworkflowmonitorevent";
        PaaSWorkflowMonitorEvent event = new PaaSWorkflowMonitorEvent();
        event.setDeploymentId(context.getDeploymentId());
        event.setExecutionId(executionId);
        template.convertAndSend(topicName,event);
    }
}