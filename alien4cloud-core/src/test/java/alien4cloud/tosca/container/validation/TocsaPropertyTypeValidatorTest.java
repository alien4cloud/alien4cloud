package alien4cloud.tosca.container.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.tosca.container.model.type.ToscaType;
import alien4cloud.tosca.model.PropertyDefinition;

public class TocsaPropertyTypeValidatorTest {
    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();;

    private PropertyDefinition createDefinitions() {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaType.STRING.toString());
        return propertyDefinition;
    }

    @Test
    public void validPropertyTypeShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions());
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidPropertyTypeShouldCreateViolations() {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("unknwon type");
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(propertyDefinition);
        Assert.assertEquals(1, violations.size());
    }
}