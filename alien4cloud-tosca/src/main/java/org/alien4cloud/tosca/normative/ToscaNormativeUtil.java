package org.alien4cloud.tosca.normative;

import java.util.List;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.utils.AlienConstants;
import alien4cloud.utils.AlienUtils;

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

    public static String formatedOperationOutputName(String nodeName, String interfaceName, String operationName, String output) {
        return AlienUtils.prefixWith(AlienConstants.OPERATION_NAME_SEPARATOR, output, new String[] { nodeName, interfaceName, operationName });
    }
}
