package alien4cloud.tosca.container.validation;

import java.util.HashMap;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.tosca.container.model.Definitions;
import alien4cloud.tosca.container.model.type.NodeType;
import alien4cloud.tosca.container.model.type.PropertyDefinition;
import alien4cloud.tosca.container.model.type.ToscaType;

public class TocsaPropertyTypeValidatorTest {
    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();;

    private Definitions createDefinitions() {
        Definitions definitions = new Definitions();
        NodeType nodeType = new NodeType();
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaType.STRING.toString());
        nodeType.setProperties(new HashMap<String, PropertyDefinition>());
        nodeType.getProperties().put("propertyKey", propertyDefinition);
        definitions.getNodeTypes().put("nodeType", nodeType);
        return definitions;
    }

    @Test
    public void validPropertyTypeShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions());
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidPropertyTypeShouldCreateViolations() {
        Definitions definitions = createDefinitions();
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("unknwon type");
        definitions.getNodeTypes().get("nodeType").getProperties().put("wrongPropertyKey", propertyDefinition);
        Set<ConstraintViolation<Definitions>> violations = validator.validate(definitions);
        Assert.assertEquals(1, violations.size());
    }
}