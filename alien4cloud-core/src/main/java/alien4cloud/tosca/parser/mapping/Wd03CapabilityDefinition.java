package alien4cloud.tosca.parser.mapping;

import java.util.Collection;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.model.CapabilityDefinition;
import alien4cloud.tosca.parser.impl.advanced.BoundParser;
import alien4cloud.tosca.parser.impl.advanced.TypeReferenceParserFactory;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

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
        quickMap(new MapParser<Collection<String>>(new ListParser<String>(getScalarParser(), "capabilities properties"), "properties"), "properties");
        quickMap(boundParser, "upperBound");
        quickMap("description");
    }
}