package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.tosca.model.AttributeDefinition;
import alien4cloud.tosca.parser.MapParser;
import alien4cloud.tosca.parser.TypeNodeParser;

@Component
public class Wd03CapabilityType extends Wd03InheritableToscaElement<IndexedCapabilityType> {
    @Resource
    private Wd03AttributeDefinition attributeDefinition;

    public Wd03CapabilityType() {
        super(new TypeNodeParser<IndexedCapabilityType>(IndexedCapabilityType.class, "Relationship type"), IndexedCapabilityType.class);
    }

    @Override
    public void initMapping() {
        super.initMapping();
        quickMap(new MapParser<AttributeDefinition>(attributeDefinition.getParser(), "Attributes"), "attributes");
    }
}