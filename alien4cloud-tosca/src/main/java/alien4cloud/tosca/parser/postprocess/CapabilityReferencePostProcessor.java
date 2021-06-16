package alien4cloud.tosca.parser.postprocess;

import alien4cloud.tosca.parser.ParsingErrorLevel;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.springframework.stereotype.Component;

/**
 * Post process references with a tolerance over a node type.
 */
@Component
public class CapabilityReferencePostProcessor extends ReferencePostProcessor {

    public void process(ParsingErrorLevel parsingErrorLevel, TypeReference typeReference) {
        typeReference.setClasses(new Class[] { CapabilityType.class });
        super.process(parsingErrorLevel, typeReference);
    }

    @Override
    public void process(TypeReference typeReference) {
        typeReference.setClasses(new Class[] { CapabilityType.class });
        super.process(typeReference);
    }
}
