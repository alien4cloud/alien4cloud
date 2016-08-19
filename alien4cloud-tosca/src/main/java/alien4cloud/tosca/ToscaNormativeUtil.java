package alien4cloud.tosca;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;

/**
 * Utility to work with normative constants.
 */
public final class ToscaNormativeUtil {

    private ToscaNormativeUtil() {
    };

    /**
     * Convert a short-named normative interface name to a long one.
     * 
     * @param interfaceName The name of the interface.
     * @return If the interface name is a normative interface shortname then the fullname, if returns the interfaceName.
     */
    public static String getLongInterfaceName(String interfaceName) {
        if (ToscaNodeLifecycleConstants.STANDARD_SHORT.equalsIgnoreCase(interfaceName)) {
            return ToscaNodeLifecycleConstants.STANDARD;
        } else if (ToscaRelationshipLifecycleConstants.CONFIGURE_SHORT.equalsIgnoreCase(interfaceName)) {
            return ToscaRelationshipLifecycleConstants.CONFIGURE;
        }
        return interfaceName;
    }
}
