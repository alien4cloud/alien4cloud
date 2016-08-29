package alien4cloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.tosca.parser.ParsingContextExecution;

/**
 * Post process references with a tolerance over a node type.
 */
@Component
public class CapabilityReferencePostProcessor extends ReferencePostProcessor {
    @Override
    public void process( TypeReference typeReference) {
        typeReference.setClasses(new Class[] { IndexedCapabilityType.class });
        super.process( typeReference);
    }
}