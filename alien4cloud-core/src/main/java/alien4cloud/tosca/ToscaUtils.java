package alien4cloud.tosca;

import java.util.List;

import alien4cloud.common.AlienConstants;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import alien4cloud.paas.exception.PaaSTechnicalException;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.tosca.normative.NormativeComputeConstants;
import alien4cloud.utils.AlienUtils;

import com.google.common.collect.Lists;

public class ToscaUtils {

    private ToscaUtils() {
    }

    /**
     * Verify that the given {@link AbstractInheritableToscaType} is from the given type.
     *
     * @param indexedInheritableToscaElement The {@link AbstractInheritableToscaType} to verify.
     * @param type The type to match
     * @return <code>true</code> if the {@link AbstractInheritableToscaType} is from the given type.
     */
    public static boolean isFromType(String type, AbstractInheritableToscaType indexedInheritableToscaElement) {
        return isFromType(type, indexedInheritableToscaElement.getElementId(), indexedInheritableToscaElement.getDerivedFrom());
    }

    /**
     * Verify that the given <code>type</code> is or inherits the given <code>expectedType</code>.
     */
    public static boolean isFromType(String expectedType, String type, List<String> typeHierarchy) {
        return expectedType.equals(type) || (typeHierarchy != null && typeHierarchy.contains(expectedType));
    }

    /**
     * Return
     *
     * @param paaSNodeTemplate
     * @return
     */
    public static PaaSNodeTemplate getMandatoryHostTemplate(final PaaSNodeTemplate paaSNodeTemplate) {
        PaaSNodeTemplate nodeTemplate = getHostTemplate(paaSNodeTemplate);
        if (nodeTemplate == null) {
            throw new PaaSTechnicalException("Cannot get the service name: The node template <" + paaSNodeTemplate.getId()
                    + "> is not declared as hosted on a compute.");
        } else {
            return nodeTemplate;
        }
    }

    public static PaaSNodeTemplate getHostTemplate(PaaSNodeTemplate paaSNodeTemplate) {
        while (paaSNodeTemplate != null) {
            if (isFromType(NormativeComputeConstants.COMPUTE_TYPE, paaSNodeTemplate.getIndexedToscaElement())) {
                // Found the compute
                return paaSNodeTemplate;
            } else {
                // Not found then go to the parent
                paaSNodeTemplate = paaSNodeTemplate.getParent();
            }
        }
        return null;
    }

    public static String formatedOperationOutputName(String nodeName, String interfaceName, String operationName, String output) {
        return AlienUtils.prefixWith(AlienConstants.OPERATION_NAME_SEPARATOR, output, new String[] { nodeName, interfaceName, operationName });
    }
}
