package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedRelationshipType;
import alien4cloud.tosca.container.model.type.Interface;
import alien4cloud.tosca.parser.ListParser;
import alien4cloud.tosca.parser.MapParser;
import alien4cloud.tosca.parser.TypeNodeParser;
import alien4cloud.tosca.parser.impl.InterfaceParser;

@Component
public class Wd03RelationshipType extends Wd03InheritableToscaElement<IndexedRelationshipType> {
    @Resource
    private Wd03AttributeDefinition attributeDefinition;
    @Resource
    private InterfaceParser interfaceParser;

    public Wd03RelationshipType() {
        super(new TypeNodeParser<IndexedRelationshipType>(IndexedRelationshipType.class, "Relationship type"), IndexedRelationshipType.class);
    }

    @Override
    public void initMapping() {
        super.initMapping();
        quickMap(attributeDefinition.getParser(), "attributes");
        quickMap(new MapParser<Interface>(interfaceParser, "interfaces"), "interfaces");
        quickMap(new ListParser<String>(getScalarParser(), "valid targets"), "validTargets");
        quickMap(new ListParser<String>(getScalarParser(), "valid sources"), "validSources");
    }
}