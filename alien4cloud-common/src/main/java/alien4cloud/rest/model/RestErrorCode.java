package alien4cloud.rest.model;

/**
 * Error codes for rest services.
 */
public enum RestErrorCode {
    // Authentication
    AUTHENTICATION_REQUIRED_ERROR(100),
    AUTHENTICATION_FAILED_ERROR(101),
    UNAUTHORIZED_ERROR(102),

    // CSAR processing errors
    CSAR_PARSING_ERROR(200),
    CSAR_INVALID_ERROR(201),

    // Indexing global error.
    INDEXING_SERVICE_ERROR(300),

    // Plugin errors
    PLUGIN_USED_ERROR(350),
    MISSING_PLUGIN_ERROR(351),
    INVALID_PLUGIN_CONFIGURATION(352),

    // Cloud errors
    CLOUD_DISABLED_ERROR(370),
    NODE_OPERATION_EXECUTION_ERROR(371),

    // Repository service error
    REPOSITORY_SERVICE_ERROR(400),
    REPOSITORY_CSAR_ALREADY_EXISTED_ERROR(401),

    // Global errors
    UNCATEGORIZED_ERROR(500),
    ILLEGAL_PARAMETER(501),
    ALREADY_EXIST_ERROR(502),
    IMAGE_UPLOAD_ERROR(503),
    NOT_FOUND_ERROR(504),
    ILLEGAL_STATE_OPERATION(505),
    INTERNAL_OBJECT_ERROR(506),
    DELETE_REFERENCED_OBJECT_ERROR(507),

    // Application handling errors : code 600+
    APPLICATION_UNDEPLOYMENT_ERROR(602),
    APPLICATION_DEPLOYMENT_ERROR(601),
    INVALID_DEPLOYMENT_SETUP(603),

    // Component handling errors : code 700+
    COMPONENT_MISSING_ERROR(700),
    COMPONENT_INTERNALTAG_ERROR(701),

    // Topology management errors.
    // Node template properties handling errors
    PROPERTY_CONSTRAINT_VIOLATION_ERROR(800),
    PROPERTY_CONSTRAINT_MATCH_ERROR(801),
    PROPERTY_MISSING_ERROR(802),
    VERSION_CONFLICT_ERROR(803),
    PROPERTY_TYPE_VIOLATION_ERROR(804),
    PROPERTY_REQUIRED_VIOLATION_ERROR(805),
    // bounds on the requirements or capabilities
    UPPER_BOUND_REACHED(810),
    LOWER_BOUND_NOT_SATISFIED(811);

    private final int code;

    private RestErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}