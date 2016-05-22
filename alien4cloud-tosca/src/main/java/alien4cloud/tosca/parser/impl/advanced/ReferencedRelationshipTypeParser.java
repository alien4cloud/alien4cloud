package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedRelationshipType;

/**
 *
 */
@Component
public class ReferencedRelationshipTypeParser extends ReferencedToscaTypeParser {
    public ReferencedRelationshipTypeParser() {
        super(IndexedRelationshipType.class);
    }
}