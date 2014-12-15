package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.component.model.IndexedArtifactType;
import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedRelationshipType;

/**
 *
 */
@Component
public class ReferencedArtifactTypeParser extends ReferencedToscaTypeParser {
    public ReferencedArtifactTypeParser() {
        super(IndexedArtifactType.class);
    }
}