package alien4cloud.paas.plan;

/**
 * Constants for TOSCA default lifecycle interfaces.
 */
public final class PlanGeneratorConstants {
    public static final String NODE_LIFECYCLE_INTERFACE_NAME = "lifecycle";
    public static final String STATE_UNKNOWN = "unknown";
    public static final String STATE_CREATING = "creating";
    public static final String STATE_CREATED = "created";
    public static final String STATE_CONFIGURING = "configuring";
    public static final String STATE_CONFIGURED = "configured";
    public static final String STATE_STARTING = "starting";
    public static final String STATE_STARTED = "started";
    public static final String STATE_STOPPING = "stopping";
    public static final String STATE_STOPPED = "stopped";
    public static final String STATE_DELETING = "deleting";
    public static final String STATE_DELETED = "deleted";
    public static final String STATE_ALREADY_STARTED = "already started";

    public static final String CREATE_OPERATION_NAME = "create";
    public static final String CONFIGURE_OPERATION_NAME = "configure";
    public static final String START_OPERATION_NAME = "start";
    public static final String STOP_OPERATION_NAME = "stop";
    public static final String DELETE_OPERATION_NAME = "delete";

    public static final String RELATIONSHIP_LIFECYCLE_INTERFACE_NAME = "tosca.interfaces.relationship.Configure";
    public static final String PRE_CONFIGURE_SOURCE = "pre_configure_source";
    public static final String PRE_CONFIGURE_TARGET = "pre_configure_target";
    public static final String POST_CONFIGURE_SOURCE = "post_configure_source";
    public static final String POST_CONFIGURE_TARGET = "post_configure_target";
    public static final String ADD_TARGET = "add_target";
    public static final String REMOVE_TARGET = "remove_target";

    private PlanGeneratorConstants() {
    }
}