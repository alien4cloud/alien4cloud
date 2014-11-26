package alien4cloud.tosca.parser.mapping;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.model.AttributeDefinition;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

@Component
public class Wd03AttributeDefinition extends AbstractMapper<AttributeDefinition> {

    public Wd03AttributeDefinition() {
        super(new TypeNodeParser<AttributeDefinition>(AttributeDefinition.class, "Attribute definition"));
    }

    @Override
    public void initMapping() {
        quickMap("type");
        quickMap("description");
        quickMap("default");
    }
}