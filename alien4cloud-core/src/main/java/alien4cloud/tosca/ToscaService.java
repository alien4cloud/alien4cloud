package alien4cloud.tosca;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.container.model.NormativeComputeConstants;

@Component
public class ToscaService {

    public boolean isCompute(String nodeTypeName, IndexedNodeType nodeType) {
        if (NormativeComputeConstants.COMPUTE_TYPE.equals(nodeTypeName)) {
            return true;
        } else {
            return nodeType.getDerivedFrom() != null && nodeType.getDerivedFrom().contains(NormativeComputeConstants.COMPUTE_TYPE);
        }
    }
}
