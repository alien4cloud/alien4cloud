package alien4cloud.topology.task;

public enum TaskCode {
    /* This code is to be used when a task is actually just there to dispatch a log message. */
    LOG,

    /** Topology validation codes */
    EMPTY,
    IMPLEMENT_RELATIONSHIP,
    SATISFY_LOWER_BOUND,
    PROPERTIES,
    SCALABLE_CAPABILITY_INVALID,
    NODE_FILTER_INVALID,
    WORKFLOW_INVALID,
    ARTIFACT_INVALID,
    DEPRECATED_NODE,

    /** Inputs codes */
    MISSING_VARIABLES,
    UNRESOLVABLE_PREDEFINED_INPUTS,
    PREDEFINED_INPUTS_CONSTRAINT_VIOLATION,
    PREDEFINED_INPUTS_TYPE_VIOLATION,
    INPUT_PROPERTY,
    INPUT_ARTIFACT_INVALID,

    /* Location policies */
    LOCATION_POLICY,
    LOCATION_UNAUTHORIZED,
    LOCATION_DISABLED,

    /* No matching node found on location for criterias */
    NO_NODE_MATCHES,
    NODE_NOT_SUBSTITUTED,
    FORBIDDEN_OPERATION,

    /** Post matching errors. */
    IMPLEMENT,
    REPLACE,

    ORCHESTRATOR_PROPERTY,

    /** Specific code for cloudify */
    CFY_MULTI_RELATIONS
}
