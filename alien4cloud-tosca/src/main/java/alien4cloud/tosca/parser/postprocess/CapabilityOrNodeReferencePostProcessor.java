package alien4cloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;

/**
 * Post process references with a tolerance over a capability type or a node type.
 */
@Component
public class CapabilityOrNodeReferencePostProcessor extends ReferencePostProcessor {
    @Override
    public void process(TypeReference typeReference) {
        typeReference.setClasses(new Class[] { CapabilityType.class, NodeType.class });
        super.process(typeReference);
    }
}