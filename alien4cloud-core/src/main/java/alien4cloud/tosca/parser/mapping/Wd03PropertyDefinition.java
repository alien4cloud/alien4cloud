package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.container.model.type.PropertyConstraint;
import alien4cloud.tosca.model.PropertyDefinition;
import alien4cloud.tosca.parser.ListParser;
import alien4cloud.tosca.parser.TypeNodeParser;
import alien4cloud.tosca.parser.impl.ConstraintParser;

@Component
public class Wd03PropertyDefinition extends AbstractMapper<PropertyDefinition> {
    @Resource
    private ConstraintParser constraintParser;
    @Resource
    private Wd03Schema schema;

    public Wd03PropertyDefinition() {
        super(new TypeNodeParser<PropertyDefinition>(PropertyDefinition.class, "Property definition"));
    }

    @Override
    public void initMapping() {
        quickMap("type");
        quickMap("required");
        quickMap("description");
        quickMap("default");
        quickMap(new ListParser<PropertyConstraint>(constraintParser, "Constraints"), "constraints");
        quickMap(schema.getParser(), "entrySchema");
    }
}