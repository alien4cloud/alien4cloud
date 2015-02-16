package alien4cloud.paas.plan;

public final class ToscaRelationshipLifecycleConstants {
    // Normative interface name
    public static final String CONFIGURE_SHORT = "Configure";
    public static final String CONFIGURE = "tosca.interfaces.relationship.Configure";

    // Normative operations
    public static final String PRE_CONFIGURE_SOURCE = "pre_configure_source";
    public static final String PRE_CONFIGURE_TARGET = "pre_configure_target";
    public static final String POST_CONFIGURE_SOURCE = "post_configure_source";
    public static final String POST_CONFIGURE_TARGET = "post_configure_target";
    public static final String ADD_TARGET = "add_target";
    public static final String ADD_SOURCE = "add_source";
    public static final String REMOVE_TARGET = "remove_target";
    public static final String REMOVE_SOURCE = "remove_source";
}