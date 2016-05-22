package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.model.components.IndexedCapabilityType;
import org.springframework.stereotype.Component;

/**
 * Parser that validates that the given string is a valid node type that exists either in the definition or one of it's dependency.
 */
@Component
public class DerivedFromCapabilityTypeParser extends DerivedFromParser {
    public DerivedFromCapabilityTypeParser() {
        super(IndexedCapabilityType.class);
    }
}