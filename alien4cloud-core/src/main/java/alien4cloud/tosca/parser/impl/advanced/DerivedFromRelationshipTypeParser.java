package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedRelationshipType;

/**
 * Parser that validates that the given string is a valid node type that exists either in the definition or one of it's dependency.
 */
@Component
public class DerivedFromRelationshipTypeParser extends DerivedFromParser {
    public DerivedFromRelationshipTypeParser() {
        super(IndexedRelationshipType.class);
    }
}