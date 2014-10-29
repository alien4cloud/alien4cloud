package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedRelationshipType;
import alien4cloud.tosca.parser.TypeNodeParser;

@Component
public class Wd03RelationshipType extends Wd03InheritableToscaElement<IndexedRelationshipType> {
    @Resource
    private Wd03AttributeDefinition attributeDefinition;

    public Wd03RelationshipType() {
        super(new TypeNodeParser<IndexedRelationshipType>(IndexedRelationshipType.class, "Relationship type"), IndexedRelationshipType.class);
    }

    @Override
    public void initMapping() {
        super.initMapping();
        quickMap(attributeDefinition.getParser(), "attributes");

    }
}