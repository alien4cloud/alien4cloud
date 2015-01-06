package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.model.components.IndexedArtifactType;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class ReferencedArtifactTypeParser extends ReferencedToscaTypeParser {
    public ReferencedArtifactTypeParser() {
        super(IndexedArtifactType.class);
    }
}