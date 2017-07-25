package alien4cloud.topology.task;

public enum TaskCode {
    /* This code is to be used when a task is actually just there to dispatch a log message. */
    LOG,
    IMPLEMENT,
    IMPLEMENT_RELATIONSHIP,
    REPLACE,
    SATISFY_LOWER_BOUND,
    PROPERTIES,
    HA_INVALID,
    SCALABLE_CAPABILITY_INVALID,
    NODE_FILTER_INVALID,
    WORKFLOW_INVALID,

    INPUT_ARTIFACT_INVALID,
    ARTIFACT_INVALID,

    /* Location policies */
    LOCATION_POLICY,
    LOCATION_UNAUTHORIZED,
    LOCATION_DISABLED,
    /* No matching node found on location for criterias */
    NO_NODE_MATCHES,

    ORCHESTRATOR_PROPERTY,
    INPUT_PROPERTY,
    NODE_NOT_SUBSTITUTED,

    FORBIDDEN_OPERATION
}
