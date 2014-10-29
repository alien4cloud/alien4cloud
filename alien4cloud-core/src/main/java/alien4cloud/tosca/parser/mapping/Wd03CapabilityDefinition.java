package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.parser.TypeNodeParser;
import alien4cloud.tosca.parser.TypeReferenceParserFactory;
import alien4cloud.tosca.parser.impl.BoundParser;

@Component
public class Wd03CapabilityDefinition extends AbstractMapper<CapabilityDefinition> {
    @Resource
    private TypeReferenceParserFactory typeReferenceParserFactory;
    @Resource
    private BoundParser boundParser;

    public Wd03CapabilityDefinition() {
        super(new TypeNodeParser<CapabilityDefinition>(CapabilityDefinition.class, "Capability definition"));
    }

    @Override
    public void initMapping() {
        quickMap(typeReferenceParserFactory.getTypeReferenceParser(IndexedCapabilityType.class, IndexedNodeType.class), "type");
        quickMap(boundParser, "upperBound");
        quickMap("properties");
    }
}