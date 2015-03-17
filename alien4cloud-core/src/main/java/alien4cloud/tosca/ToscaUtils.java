package alien4cloud.tosca;

import java.util.List;

import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.paas.exception.PaaSTechnicalException;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.tosca.normative.NormativeComputeConstants;

import com.google.common.collect.Lists;

public class ToscaUtils {

    private ToscaUtils() {
    }

    /**
     * Verify that the given {@link IndexedInheritableToscaElement} is from the given type.
     *
     * @param indexedInheritableToscaElement The {@link IndexedInheritableToscaElement} to verify.
     * @param type The type to match
     * @return <code>true</code> if the {@link IndexedInheritableToscaElement} is from the given type.
     */
    public static boolean isFromType(String type, IndexedInheritableToscaElement indexedInheritableToscaElement) {
        return type.equals(indexedInheritableToscaElement.getElementId())
                || (indexedInheritableToscaElement.getDerivedFrom() != null && indexedInheritableToscaElement.getDerivedFrom().contains(type));
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

    /**
     * Returns the ordered nodeTemplate hierarchy for a given nodeTemplate
     * 
     * @param paaSNodeTemplate
     * @return ordered nodeTemplate map
     */
    public static List<PaaSNodeTemplate> getParents(final PaaSNodeTemplate paaSNodeTemplate) {
        PaaSNodeTemplate parent = paaSNodeTemplate;
        List<PaaSNodeTemplate> templateList = Lists.newArrayList();
        while (parent != null) {
            parent = parent.getParent();
            if (parent != null) {
                templateList.add(parent);
            }
        }
        if (templateList.isEmpty()) {
            // youy nodeTemplate must be a compute => there is no host
            throw new PaaSTechnicalException("The node template <" + paaSNodeTemplate.getId() + "> is not declared as hosted on a compute.");
        }
        return templateList;
    }

}
