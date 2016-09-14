package alien4cloud.tosca.parser.postprocess;

import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.types.CapabilityType;

/**
 * Post process references with a tolerance over a node type.
 */
@Component
public class CapabilityReferencePostProcessor extends ReferencePostProcessor {
    @Override
    public void process( TypeReference typeReference) {
        typeReference.setClasses(new Class[] { CapabilityType.class });
        super.process( typeReference);
    }
}