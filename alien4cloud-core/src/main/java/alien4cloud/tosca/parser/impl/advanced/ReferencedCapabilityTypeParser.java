package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedNodeType;

/**
 *
 */
@Component
public class ReferencedCapabilityTypeParser extends ReferencedToscaTypeParser {
    public ReferencedCapabilityTypeParser() {
        super(IndexedCapabilityType.class, IndexedNodeType.class);
    }
}