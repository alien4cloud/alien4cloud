package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;

/**
 *
 */
@Component
public class ReferencedCapabilityTypeParser extends ReferencedToscaTypeParser {
    public ReferencedCapabilityTypeParser() {
        super(IndexedCapabilityType.class, IndexedNodeType.class);
    }
}