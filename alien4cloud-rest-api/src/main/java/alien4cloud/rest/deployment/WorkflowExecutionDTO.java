package alien4cloud.rest.deployment;

import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.runtime.Execution;
import alien4cloud.model.runtime.Task;
import alien4cloud.model.runtime.WorkflowStepInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * A snapshot of a workflow execution at a given time.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionDTO {

    /**
     * The status of a step is related to the status of it's related instances:
     * <ul>
     *     <li>If one instance is STARTED then the step is STARTED.</li>
     *     <li>If all instance are COMPLETED without error then the step is COMPLETED_SUCCESSFULL.</li>
     *     <li>If all instance are COMPLETED but one has error, then the step is COMPLETED_WITH_ERROR.</li>
     * </ul>
     */
    public enum WorkflowExecutionStepStatus {
        STARTED,
        COMPLETED_SUCCESSFULL,
        COMPLETED_WITH_ERROR
    }

    private Execution execution;

    /**
     * The number of step instances currently known (used for progress information).
     */
    private int actualKnownStepInstanceCount;

    private Task lastKnownExecutingTask;

    /**
     * The step instances per stepId.
     */
    private Map<String, WorkflowExecutionStepStatus> stepStatus;

    /**
     * The step instances per stepId.
     */
    private Map<String, List<WorkflowStepInstance>> stepInstances;

    /**
     * The steps per stepId.
     */
    private Map<String, List<Task>> stepTasks;

}
