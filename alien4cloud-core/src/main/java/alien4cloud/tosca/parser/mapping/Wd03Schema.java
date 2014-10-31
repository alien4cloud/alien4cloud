package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;
import javax.validation.Validator;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.model.PropertyDefinition;
import alien4cloud.tosca.model.Schema;
import alien4cloud.tosca.parser.MapParser;
import alien4cloud.tosca.parser.TypeNodeParser;
import alien4cloud.tosca.parser.ValidatedNodeParser;
import alien4cloud.tosca.parser.impl.ConstraintParser;

@Component
public class Wd03Schema extends AbstractMapper<Schema> {
    @Resource
    private Wd03PropertyDefinition propertyDefinition;
    @Resource
    private ConstraintParser constraintParser;
    @Resource
    private Validator validator;

    public Wd03Schema() {
        super(new TypeNodeParser<Schema>(Schema.class, "Schema"));
    }

    @Override
    public void initMapping() {
        quickMap("derivedFrom");
        quickMap(constraintParser, "constraints");
        quickMap(new MapParser<PropertyDefinition>(new ValidatedNodeParser<PropertyDefinition>(validator, propertyDefinition.getParser()), "properties"),
                "properties");
    }
}