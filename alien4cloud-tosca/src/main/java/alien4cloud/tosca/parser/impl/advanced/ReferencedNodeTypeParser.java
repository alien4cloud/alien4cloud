package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.IndexedNodeType;

@Component
public class ReferencedNodeTypeParser extends ReferencedToscaTypeParser {
    public ReferencedNodeTypeParser() {
        super(IndexedNodeType.class);
    }
}