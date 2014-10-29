package alien4cloud.tosca.container.validation;

import java.util.HashMap;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import alien4cloud.tosca.container.model.Definitions;
import alien4cloud.tosca.container.model.type.NodeType;
import alien4cloud.tosca.container.model.type.ToscaType;
import alien4cloud.tosca.model.PropertyDefinition;

public class TocsaPropertyDefaultValueTypeValidatorTest {
    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();;

    private Definitions createDefinitions(String propertyType, String defaultValue) {
        Definitions definitions = new Definitions();
        NodeType nodeType = new NodeType();
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(propertyType);
        propertyDefinition.setDefault(defaultValue);
        nodeType.setProperties(new HashMap<String, PropertyDefinition>());
        nodeType.getProperties().put("propertyKey", propertyDefinition);
        definitions.getNodeTypes().put("nodeType", nodeType);
        return definitions;
    }

    @Test
    public void validStringDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.STRING.toString(), "string value"));
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void validBooleanDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.BOOLEAN.toString(), "true"));
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidBooleanDefaultShouldCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.BOOLEAN.toString(), "not a boolean"));
        Assert.assertEquals(1, violations.size());
    }

    @Test
    public void validFloatDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.FLOAT.toString(), "6.5"));
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatDefaultShouldCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.FLOAT.toString(), "not a float"));
        Assert.assertEquals(1, violations.size());
    }

    @Test
    public void validIntegerDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.INTEGER.toString(), "4"));
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerDefaultShouldCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.INTEGER.toString(), "not an integer"));
        Assert.assertEquals(1, violations.size());
    }

    @Ignore
    @Test
    public void validCanonicalTimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.TIMESTAMP.toString(), "2001-12-15T02:59:43.1Z"));
        Assert.assertEquals(0, violations.size());
    }

    @Ignore
    @Test
    public void validIso8601TimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator
                .validate(createDefinitions(ToscaType.TIMESTAMP.toString(), "2001-12-14t21:59:43.10-05:00"));
        Assert.assertEquals(0, violations.size());
    }

    @Ignore
    @Test
    public void validSpaceSeparatedTimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.TIMESTAMP.toString(), "2001-12-14 21:59:43.10 -5"));
        Assert.assertEquals(0, violations.size());
    }

    @Ignore
    @Test
    public void validNoTimezoneTimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.TIMESTAMP.toString(), "2001-12-15 2:59:43.10"));
        Assert.assertEquals(0, violations.size());
    }

    @Ignore
    @Test
    public void validDateTimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.TIMESTAMP.toString(), "2002-12-14"));
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidTimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.TIMESTAMP.toString(), "2001-12-14 21:59:43.10 Z-5"));
        Assert.assertEquals(1, violations.size());
    }

    @Test
    public void validVersionDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.VERSION.toString(), "1.3.15"));
        Assert.assertEquals(0, violations.size());
    }

    // Versions are not managed currently.
    @Ignore
    @Test
    public void invalidVersionDefaultShouldCreateViolations() {
        Set<ConstraintViolation<Definitions>> violations = validator.validate(createDefinitions(ToscaType.VERSION.toString(), "not a version"));
        Assert.assertEquals(1, violations.size());
    }
}