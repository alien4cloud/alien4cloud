package alien4cloud.rest.deployment;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.deployment.ExecutionService;
import alien4cloud.deployment.TaskService;
import alien4cloud.deployment.WorkflowStepInstanceService;
import alien4cloud.model.runtime.Execution;
import alien4cloud.model.runtime.Task;
import alien4cloud.model.runtime.TaskStatus;
import alien4cloud.model.runtime.WorkflowStepInstance;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({ "/rest/workflow_execution", "/rest/v1/workflow_execution", "/rest/latest/workflow_execution" })
@Api(value = "", description = "An endpoint to monitor workflow executions")
public class WorkflowExecutionController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ExecutionService executionService;
    @Resource
    private TaskService taskService;
    @Resource
    private WorkflowStepInstanceService workflowStepInstanceService;

    /**
     * For a given deployment, get the last workflow execution monitor data.
     */
    @ApiOperation(value = "Search for last workflow execution monitor data", notes = "For a given deployment, get the last workflow execution monitor data.")
    @RequestMapping(value = "/{deploymentId}", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<WorkflowExecutionDTO> getLastWorkflowExecution(@ApiParam(value = "Deployment id.", required = true) @Valid @NotBlank @PathVariable String deploymentId) {

        WorkflowExecutionDTO result = new WorkflowExecutionDTO();
        Execution execution = executionService.getLastExecution(deploymentId);
        if (execution == null) {
            return RestResponseBuilder.<WorkflowExecutionDTO> builder().data(result).build();
        }

        FacetedSearchResult<Task> tasksResult = taskService.searchTasks("", execution.getId(), 0, Integer.MAX_VALUE);
        FacetedSearchResult<WorkflowStepInstance> stepsResult = workflowStepInstanceService.searchInstances("", execution.getId(), 0, Integer.MAX_VALUE);

        result.setExecution(execution);
        Map<String, List<WorkflowStepInstance>> stepInstances = Maps.newHashMap();
        result.setStepInstances(stepInstances);
        Map<String, List<Task>> tasks = Maps.newHashMap();
        result.setStepTasks(tasks);
        Map<String, WorkflowExecutionDTO.WorkflowExecutionStepStatus> stepStatuses = Maps.newHashMap();
        result.setStepStatus(stepStatuses);

        // just a temporary map to store WorkflowStepInstances per id
        Map<String, WorkflowStepInstance> _stepsInstanceIds = Maps.newHashMap();

        // the number of step instances is used to display progress information
        result.setActualKnownStepInstanceCount(stepsResult.getData().length);

        // populate step instances
        for (WorkflowStepInstance stepInstance : stepsResult.getData()) {
            if (StringUtils.isEmpty(stepInstance.getStepId())) {
                // FIXME: understand in which circumstances this id can be null
                continue;
            }
            _stepsInstanceIds.put(stepInstance.getId(), stepInstance);
            List<WorkflowStepInstance> workflowStepInstances = result.getStepInstances().get(stepInstance.getStepId());
            if (workflowStepInstances == null) {
                workflowStepInstances = Lists.newLinkedList();
                result.getStepInstances().put(stepInstance.getStepId(), workflowStepInstances);
            }
            workflowStepInstances.add(stepInstance);
        }
        // populate step tasks
        for (Task task : tasksResult.getData()) {
            // we want the last executing task to be emphased
            if (task.getStatus() == TaskStatus.STARTED || task.getStatus() == TaskStatus.SCHEDULED) {
                Task lastKnownTask = result.getLastKnownExecutingTask();
                if (lastKnownTask == null || lastKnownTask.getScheduleDate().before(task.getScheduleDate())) {
                    result.setLastKnownExecutingTask(task);
                }
            }
            if (task.getWorkflowStepInstanceId() == null) {
                continue;
            }
            WorkflowStepInstance workflowStepInstance = _stepsInstanceIds.get(task.getWorkflowStepInstanceId());
            if (workflowStepInstance == null || StringUtils.isEmpty(workflowStepInstance.getId()) ) {
                // this task is not related to any workflow step instance, just forget it
                continue;
            }
            List<Task> tasksPerStepId = result.getStepTasks().get(workflowStepInstance.getStepId());
            if (tasksPerStepId == null) {
                tasksPerStepId = Lists.newLinkedList();
                result.getStepTasks().put(workflowStepInstance.getStepId(), tasksPerStepId);
            }
            tasksPerStepId.add(task);
        }
        // compute step status
        // The status of a step is related to the status of it's related instances:
        // - If one instance is STARTED then the step is STARTED.
        // - If all instance are COMPLETED without error then the step is COMPLETED_SUCCESSFULL.
        // - If all instance are COMPLETED but one has error, then the step is COMPLETED_WITH_ERROR.
        result.getStepInstances().forEach((stepId, workflowStepInstances) -> {
            WorkflowExecutionDTO.WorkflowExecutionStepStatus stepStatus = result.getStepStatus().get(stepId);

            for (WorkflowStepInstance workflowStepInstance : workflowStepInstances) {
                if (stepStatus == null) {
                    switch (workflowStepInstance.getStatus()) {
                        case COMPLETED:
                            stepStatus = workflowStepInstance.isHasFailedTasks() ? WorkflowExecutionDTO.WorkflowExecutionStepStatus.COMPLETED_WITH_ERROR : WorkflowExecutionDTO.WorkflowExecutionStepStatus.COMPLETED_SUCCESSFULL;
                            break;
                        case STARTED:
                            stepStatus = workflowStepInstance.isHasFailedTasks() ? WorkflowExecutionDTO.WorkflowExecutionStepStatus.COMPLETED_WITH_ERROR : WorkflowExecutionDTO.WorkflowExecutionStepStatus.STARTED;
                            break;
                        default:
                            stepStatus = WorkflowExecutionDTO.WorkflowExecutionStepStatus.STARTED;
                    }
                } else {
                    switch (workflowStepInstance.getStatus()) {
                        case COMPLETED:
                            if (stepStatus == WorkflowExecutionDTO.WorkflowExecutionStepStatus.COMPLETED_WITH_ERROR) {
                                // let the state as is
                            } else if (stepStatus == WorkflowExecutionDTO.WorkflowExecutionStepStatus.COMPLETED_SUCCESSFULL) {
                                stepStatus = workflowStepInstance.isHasFailedTasks() ? WorkflowExecutionDTO.WorkflowExecutionStepStatus.COMPLETED_WITH_ERROR : WorkflowExecutionDTO.WorkflowExecutionStepStatus.COMPLETED_SUCCESSFULL;
                            }
                            break;
                        case STARTED:
                            // if any step instance is STARTED then the whole step is STARTED
                            stepStatus = workflowStepInstance.isHasFailedTasks() ? WorkflowExecutionDTO.WorkflowExecutionStepStatus.COMPLETED_WITH_ERROR : WorkflowExecutionDTO.WorkflowExecutionStepStatus.STARTED;
                            break;
                        default:
                            stepStatus = WorkflowExecutionDTO.WorkflowExecutionStepStatus.STARTED;
                    }
                }
            }
            result.getStepStatus().put(stepId, stepStatus);
        });

        return RestResponseBuilder.<WorkflowExecutionDTO> builder().data(result).build();
    }

}