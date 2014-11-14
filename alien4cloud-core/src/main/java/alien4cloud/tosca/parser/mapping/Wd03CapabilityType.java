package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.tosca.model.AttributeDefinition;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

@Component
public class Wd03CapabilityType extends Wd03InheritableToscaElement<IndexedCapabilityType> {
    @Resource
    private Wd03AttributeDefinition attributeDefinition;

    public Wd03CapabilityType() {
        super(new TypeNodeParser<IndexedCapabilityType>(IndexedCapabilityType.class, "Capability type"), IndexedCapabilityType.class);
    }

    @Override
    public void initMapping() {
        super.initMapping();
        quickMap(new MapParser<AttributeDefinition>(attributeDefinition.getParser(), "Attributes"), "attributes");
    }
}