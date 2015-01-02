package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedArtifactType;

/**
 * Parser that validates that the given string is a valid node type that exists either in the definition or one of it's dependency.
 */
@Component
public class DerivedFromArtifactTypeParser extends DerivedFromParser {
    public DerivedFromArtifactTypeParser() {
        super(IndexedArtifactType.class);
    }
}