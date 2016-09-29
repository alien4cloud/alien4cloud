package alien4cloud.tosca.container.validation;

import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.Assert;
import org.junit.Test;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import alien4cloud.tosca.normative.ToscaType;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.InRangeConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MaxLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MinLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.PatternConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.ValidValuesConstraint;

import com.google.common.collect.Lists;

public class ToscaPropertyConstraintValidatorTest {
    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private PropertyDefinition createDefinitions(String propertyType, PropertyConstraint constraint) {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(propertyType);
        propertyDefinition.setConstraints(Lists.newArrayList(constraint));
        return propertyDefinition;
    }

    private PropertyDefinition createEqualDefinition(String propertyType, String value) {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual(value);
        return createDefinitions(propertyType, constraint);
    }

    private PropertyDefinition createGreaterOrEqualDefinition(String propertyType, Comparable<?> value) {
        GreaterOrEqualConstraint constraint = new GreaterOrEqualConstraint();
        constraint.setGreaterOrEqual(String.valueOf(value));
        return createDefinitions(propertyType, constraint);
    }

    private PropertyDefinition createGreaterThanDefinition(String propertyType, Comparable<?> value) {
        GreaterThanConstraint constraint = new GreaterThanConstraint();
        constraint.setGreaterThan(String.valueOf(value));
        return createDefinitions(propertyType, constraint);
    }

    @SuppressWarnings("rawtypes")
    private PropertyDefinition createInRangeDefinition(String propertyType, Comparable minValue, Comparable maxValue) {
        InRangeConstraint constraint = new InRangeConstraint();
        constraint.setInRange(Lists.newArrayList(String.valueOf(minValue), String.valueOf(maxValue)));
        return createDefinitions(propertyType, constraint);
    }

    private PropertyDefinition createLenghtDefinition(String propertyType, int value) {
        LengthConstraint constraint = new LengthConstraint();
        constraint.setLength(value);
        return createDefinitions(propertyType, constraint);
    }

    private PropertyDefinition createLessOrEqualDefinition(String propertyType, Comparable<?> value) {
        LessOrEqualConstraint constraint = new LessOrEqualConstraint();
        constraint.setLessOrEqual(String.valueOf(value));
        return createDefinitions(propertyType, constraint);
    }

    private PropertyDefinition createLessDefinition(String propertyType, Comparable<?> value) {
        LessThanConstraint constraint = new LessThanConstraint();
        constraint.setLessThan(String.valueOf(value));
        return createDefinitions(propertyType, constraint);
    }

    private PropertyDefinition createMaxLenghtDefinition(String propertyType, int value) {
        MaxLengthConstraint constraint = new MaxLengthConstraint();
        constraint.setMaxLength(value);
        return createDefinitions(propertyType, constraint);
    }

    private PropertyDefinition createMinLenghtDefinition(String propertyType, int value) {
        MinLengthConstraint constraint = new MinLengthConstraint();
        constraint.setMinLength(value);
        return createDefinitions(propertyType, constraint);
    }

    private PropertyDefinition createPatternDefinition(String propertyType, String value) {
        PatternConstraint constraint = new PatternConstraint();
        constraint.setPattern(value);
        return createDefinitions(propertyType, constraint);
    }

    private PropertyDefinition createValidValuesDefinition(String propertyType, List<String> value) {
        ValidValuesConstraint constraint = new ValidValuesConstraint();
        constraint.setValidValues(value);
        return createDefinitions(propertyType, constraint);
    }

    @Test
    public void validStringEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createEqualDefinition(ToscaType.STRING.toString(), "value"),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void validIntegerEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createEqualDefinition(ToscaType.INTEGER.toString(), "42"),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createEqualDefinition(ToscaType.INTEGER.toString(), "not an integer"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createEqualDefinition(ToscaType.FLOAT.toString(), "42.456"),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createEqualDefinition(ToscaType.FLOAT.toString(), "not a float"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void invalidStringGreaterOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterOrEqualDefinition(ToscaType.STRING.toString(), "value"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validIntegerGreaterOrEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterOrEqualDefinition(ToscaType.INTEGER.toString(), 42),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerGreaterOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createGreaterOrEqualDefinition(ToscaType.INTEGER.toString(), "not an integer"), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatGreaterOrEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterOrEqualDefinition(ToscaType.FLOAT.toString(), 42.456f),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatGreaterOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterOrEqualDefinition(ToscaType.FLOAT.toString(), "not a float"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void invalidStringGreaterThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterThanDefinition(ToscaType.STRING.toString(), "value"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validIntegerGreaterThanConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterThanDefinition(ToscaType.INTEGER.toString(), 42),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerGreaterThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createGreaterThanDefinition(ToscaType.INTEGER.toString(), "not an integer"), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatGreaterThanConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterThanDefinition(ToscaType.FLOAT.toString(), 42.456f),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatGreaterThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterThanDefinition(ToscaType.FLOAT.toString(), "not a float"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void invalidStringInRangeConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createInRangeDefinition(ToscaType.STRING.toString(), "one", "two"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validIntegerInRangeConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createInRangeDefinition(ToscaType.INTEGER.toString(), 42, 65),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerInRangeConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createInRangeDefinition(ToscaType.INTEGER.toString(), 42.456f, 65.343f),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatInRangeConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createInRangeDefinition(ToscaType.FLOAT.toString(), 42.456f, 65.343f),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatInRangeConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createInRangeDefinition(ToscaType.FLOAT.toString(), 42.456f, "not a float"), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validStringLengthConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLenghtDefinition(ToscaType.STRING.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerLengthConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLenghtDefinition(ToscaType.INTEGER.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void invalidStringLessOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessOrEqualDefinition(ToscaType.STRING.toString(), "value"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validIntegerLessOrEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessOrEqualDefinition(ToscaType.INTEGER.toString(), 42),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerLessOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createLessOrEqualDefinition(ToscaType.INTEGER.toString(), "not an integer"), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatLessOrEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessOrEqualDefinition(ToscaType.FLOAT.toString(), 42.456f),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatLessOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessOrEqualDefinition(ToscaType.FLOAT.toString(), "not a float"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void invalidStringLessThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessDefinition(ToscaType.STRING.toString(), "value"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validIntegerLessThanConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessDefinition(ToscaType.INTEGER.toString(), 42),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerLessThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessDefinition(ToscaType.INTEGER.toString(), "not an integer"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatLessThanConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessDefinition(ToscaType.FLOAT.toString(), 42.456f),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatLessThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessDefinition(ToscaType.FLOAT.toString(), "not a float"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validStringMaxLengthConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createMaxLenghtDefinition(ToscaType.STRING.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerMaxLengthConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createMaxLenghtDefinition(ToscaType.INTEGER.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validStringMinLengthConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createMinLenghtDefinition(ToscaType.STRING.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerMinLengthConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createMinLenghtDefinition(ToscaType.INTEGER.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validStringPatternConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createPatternDefinition(ToscaType.STRING.toString(), "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"), ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerPatternConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createPatternDefinition(ToscaType.INTEGER.toString(), "2"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validStringValidValuesConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createValidValuesDefinition(ToscaType.STRING.toString(), Lists.newArrayList("value", "othervalue")), ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void validIntegerValidValuesConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createValidValuesDefinition(ToscaType.INTEGER.toString(), Lists.newArrayList("42", "56")), ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerValidValuesConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createValidValuesDefinition(ToscaType.INTEGER.toString(), Lists.newArrayList("42", "not an integer")), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatValidValuesConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createValidValuesDefinition(ToscaType.FLOAT.toString(), Lists.newArrayList("42.345", "56.652")), ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatValidValuesConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createValidValuesDefinition(ToscaType.FLOAT.toString(), Lists.newArrayList("42.345", "not a float")), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }
}