package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.component.model.IndexedCapabilityType;
import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedNodeType;

/**
 * Parser that validates that the given string is a valid node type that exists either in the definition or one of it's dependency.
 */
@Component
public class DerivedFromCapabilityTypeParser extends DerivedFromParser {
    public DerivedFromCapabilityTypeParser() {
        super(IndexedCapabilityType.class);
    }
}