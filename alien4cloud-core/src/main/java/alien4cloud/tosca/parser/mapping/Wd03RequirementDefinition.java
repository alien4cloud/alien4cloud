package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.model.PropertyConstraint;
import alien4cloud.tosca.model.RequirementDefinition;
import alien4cloud.tosca.parser.KeyValueMappingTarget;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.impl.advanced.BoundParser;
import alien4cloud.tosca.parser.impl.advanced.ConstraintParser;
import alien4cloud.tosca.parser.impl.advanced.TypeReferenceParserFactory;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

@Component
public class Wd03RequirementDefinition extends AbstractMapper<RequirementDefinition> {
    @Resource
    private TypeReferenceParserFactory typeReferenceParserFactory;
    @Resource
    private BoundParser boundParser;
    @Resource
    private ConstraintParser constraintParser;

    public Wd03RequirementDefinition() {
        super(new TypeNodeParser<RequirementDefinition>(RequirementDefinition.class, "Requirement definition"));
    }

    @Override
    public void initMapping() {
        instance.getYamlOrderedToObjectMapping().put(
                0,
                new KeyValueMappingTarget("id", "type", typeReferenceParserFactory.getTypeReferenceParser(IndexedCapabilityType.class,
                        IndexedNodeType.class)));
        quickMap("lowerBound");
        quickMap(boundParser, "upperBound");
        instance.getYamlToObjectMapping().put("relationship", new MappingTarget("relationshipType", getScalarParser()));
        instance.getYamlToObjectMapping().put("capability", new MappingTarget("capabilityName", getScalarParser()));
        quickMap(new MapParser<PropertyConstraint>(constraintParser, "Constraints"), "constraints");
    }
}