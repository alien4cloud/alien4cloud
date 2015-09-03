package alien4cloud.paas.wf;

import java.util.Map;

import alien4cloud.model.components.Interface;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.tosca.normative.NormativeComputeConstants;

public class WorkflowUtils {

    private static final String NETWORK_TYPE = "tosca.nodes.Network";

    public static Interface getNodeInterface(PaaSNodeTemplate nodeTemplate, String interfaceName) {
        Interface interfaz = getInterface(interfaceName, nodeTemplate.getIndexedToscaElement().getInterfaces());
        if (interfaz == null) {
            throw new IllegalArgumentException("Plan cannot be generated as required interface <" + interfaceName + "> has not been found on node <"
                    + nodeTemplate.getNodeTemplate().getName() + "> from type <" + nodeTemplate.getNodeTemplate().getType() + ">.");
        }
        return interfaz;
    }

    public static Interface getInterface(String interfaceName, Map<String, Interface> interfaces) {
        return interfaces == null ? null : interfaces.get(interfaceName);
    }

    public static boolean isCompute(PaaSNodeTemplate paaSNodeTemplate) {
        return isOfType(paaSNodeTemplate, NormativeComputeConstants.COMPUTE_TYPE);
    }

    public static boolean isNetwork(PaaSNodeTemplate paaSNodeTemplate) {
        return isOfType(paaSNodeTemplate, NETWORK_TYPE);
    }

    private static boolean isOfType(PaaSNodeTemplate paaSNodeTemplate, String type) {
        return paaSNodeTemplate.getIndexedToscaElement().getElementId().equals(type)
                || paaSNodeTemplate.getIndexedToscaElement().getDerivedFrom().contains(type);
    }

}
