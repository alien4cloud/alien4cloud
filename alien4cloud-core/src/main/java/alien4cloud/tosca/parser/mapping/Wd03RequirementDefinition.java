package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.container.model.type.RequirementDefinition;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.TypeNodeParser;
import alien4cloud.tosca.parser.TypeReferenceParserFactory;
import alien4cloud.tosca.parser.impl.BoundParser;

@Component
public class Wd03RequirementDefinition extends AbstractMapper<RequirementDefinition> {
    @Resource
    private TypeReferenceParserFactory typeReferenceParserFactory;
    @Resource
    private BoundParser boundParser;

    public Wd03RequirementDefinition() {
        super(new TypeNodeParser<RequirementDefinition>(RequirementDefinition.class, "Requirement definition"));
    }

    @Override
    public void initMapping() {
        instance.getYamlOrderedToObjectMapping().put(0, new MappingTarget("type", getScalarParser()));
        quickMap(typeReferenceParserFactory.getTypeReferenceParser(IndexedCapabilityType.class, IndexedNodeType.class), "type");
        quickMap("lowerBound");
        quickMap(boundParser, "upperBound");
        quickMap("relationshipType");
    }
}