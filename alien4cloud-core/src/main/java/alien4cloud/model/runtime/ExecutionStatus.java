package alien4cloud.model.runtime;

/**
 * Status of an execution.
 */
public enum ExecutionStatus {
    SCHEDULED,
    RUNNING,
    SUCCEEDED,
    CANCELLED,
    FAILED
}
