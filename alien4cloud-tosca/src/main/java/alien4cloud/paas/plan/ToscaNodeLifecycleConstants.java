package alien4cloud.paas.plan;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Constants and helper functions to manage TOSCA Node life-cycle.
 */
public final class ToscaNodeLifecycleConstants {
    // lifecycle interfaces
    public static final String STANDARD_SHORT = "Standard";
    public static final String STANDARD = "tosca.interfaces.node.lifecycle.Standard";

    // lifecycle operations
    public static final String CREATE = "create";
    public static final String CONFIGURE = "configure";
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String DELETE = "delete";

    public static final String ATT_STATE = "state";

    // node states
    public static final String INITIAL = "initial";
    public static final String CREATING = "creating";
    public static final String CREATED = "created";
    public static final String CONFIGURING = "configuring";
    public static final String CONFIGURED = "configured";
    public static final String STARTING = "starting";
    public static final String STARTED = "started";
    public static final String AVAILABLE = "available";
    public static final String STOPPING = "stopping";
    public static final String STOPPED = "stopped";
    public static final String DELETING = "deleting";
    public static final String DELETED = "deleted";
    public static final String ERROR = "error";

    public static final Set<String> TOSCA_STATES = Sets.newHashSet(INITIAL, CREATING, CREATED, CONFIGURING, CONFIGURED, STARTING, STARTED, AVAILABLE, STOPPING,
            STOPPED, DELETING, DELETED, ERROR);

    private ToscaNodeLifecycleConstants() {
    }
}