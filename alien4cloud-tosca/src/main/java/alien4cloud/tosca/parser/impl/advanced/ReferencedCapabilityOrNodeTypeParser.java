package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;

@Component
public class ReferencedCapabilityOrNodeTypeParser extends ReferencedToscaTypeParser {
    public ReferencedCapabilityOrNodeTypeParser() {
        super(IndexedCapabilityType.class, IndexedNodeType.class);
    }
}
