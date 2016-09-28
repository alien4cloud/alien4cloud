package alien4cloud.tosca.container.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import alien4cloud.tosca.normative.ToscaType;

@Ignore
public class ToscaPropertyDefaultValueTypeValidatorTest {
    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();;

    private PropertyDefinition createDefinitions(String propertyType, String defaultValue) {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(propertyType);
        propertyDefinition.setDefault(new ScalarPropertyValue(defaultValue));
        return propertyDefinition;
    }

    @Test
    public void validStringDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.STRING.toString(), "string value"));
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void validBooleanDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.BOOLEAN, "true"));
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidBooleanDefaultShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.BOOLEAN, "not a boolean"));
        Assert.assertEquals(1, violations.size());
    }

    @Test
    public void validFloatDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.FLOAT, "6.5"));
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatDefaultShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.FLOAT, "not a float"));
        Assert.assertEquals(1, violations.size());
    }

    @Test
    public void validIntegerDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.INTEGER, "4"));
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerDefaultShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.INTEGER, "not an integer"));
        Assert.assertEquals(1, violations.size());
    }

    @Ignore
    @Test
    public void validCanonicalTimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator
                .validate(createDefinitions(ToscaType.TIMESTAMP, "2001-12-15T02:59:43.1Z"));
        Assert.assertEquals(0, violations.size());
    }

    @Ignore
    @Test
    public void validIso8601TimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.TIMESTAMP,
                "2001-12-14t21:59:43.10-05:00"));
        Assert.assertEquals(0, violations.size());
    }

    @Ignore
    @Test
    public void validSpaceSeparatedTimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.TIMESTAMP,
                "2001-12-14 21:59:43.10 -5"));
        Assert.assertEquals(0, violations.size());
    }

    @Ignore
    @Test
    public void validNoTimezoneTimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator
                .validate(createDefinitions(ToscaType.TIMESTAMP, "2001-12-15 2:59:43.10"));
        Assert.assertEquals(0, violations.size());
    }

    @Ignore
    @Test
    public void validDateTimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.TIMESTAMP, "2002-12-14"));
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidTimestampDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.TIMESTAMP,
                "2001-12-14 21:59:43.10 Z-5"));
        Assert.assertEquals(1, violations.size());
    }

    @Test
    public void validVersionDefaultShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.VERSION, "1.3.15"));
        Assert.assertEquals(0, violations.size());
    }

    // Versions are not managed currently.
    @Ignore
    @Test
    public void invalidVersionDefaultShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createDefinitions(ToscaType.VERSION, "not a version"));
        Assert.assertEquals(1, violations.size());
    }
}