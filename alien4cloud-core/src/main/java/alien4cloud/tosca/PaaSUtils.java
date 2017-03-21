package alien4cloud.tosca;

import alien4cloud.paas.exception.PaaSTechnicalException;
import alien4cloud.paas.model.PaaSNodeTemplate;
import org.alien4cloud.tosca.normative.ToscaNormativeUtil;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaaSUtils {
    /**
     * Return
     *
     * @param paaSNodeTemplate
     * @return
     */
    public static PaaSNodeTemplate getMandatoryHostTemplate(final PaaSNodeTemplate paaSNodeTemplate) {
        PaaSNodeTemplate nodeTemplate = getHostTemplate(paaSNodeTemplate);
        if (nodeTemplate == null) {
            throw new PaaSTechnicalException(
                    "Cannot get the service name: The node template <" + paaSNodeTemplate.getId() + "> is not declared as hosted on a compute.");
        } else {
            return nodeTemplate;
        }
    }

    public static PaaSNodeTemplate getHostTemplate(PaaSNodeTemplate paaSNodeTemplate) {
        while (paaSNodeTemplate != null) {
            if (ToscaNormativeUtil.isFromType(NormativeComputeConstants.COMPUTE_TYPE, paaSNodeTemplate.getIndexedToscaElement())) {
                // Found the compute
                return paaSNodeTemplate;
            } else {
                // Not found then go to the parent
                paaSNodeTemplate = paaSNodeTemplate.getParent();
            }
        }
        return null;
    }
}
