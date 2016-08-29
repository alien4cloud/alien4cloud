package alien4cloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.tosca.parser.ParsingContextExecution;

/**
 * Post process references with a tolerance over a capability type or a node type.
 */
@Component
public class CapabilityOrNodeReferencePostProcessor extends ReferencePostProcessor {
    @Override
    public void process(TypeReference typeReference) {
        typeReference.setClasses(new Class[] { IndexedCapabilityType.class, IndexedNodeType.class });
        super.process(typeReference);
    }
}