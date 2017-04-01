package org.alien4cloud.tosca.model.definitions.constraints;

import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import alien4cloud.tosca.container.validation.ToscaSequence;
import org.junit.Assert;
import org.junit.Test;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;

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
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createEqualDefinition(ToscaTypes.STRING.toString(), "value"),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void validIntegerEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createEqualDefinition(ToscaTypes.INTEGER.toString(), "42"),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createEqualDefinition(ToscaTypes.INTEGER.toString(), "not an integer"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createEqualDefinition(ToscaTypes.FLOAT.toString(), "42.456"),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createEqualDefinition(ToscaTypes.FLOAT.toString(), "not a float"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void invalidStringGreaterOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterOrEqualDefinition(ToscaTypes.STRING.toString(), "value"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validIntegerGreaterOrEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterOrEqualDefinition(ToscaTypes.INTEGER.toString(), 42),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerGreaterOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createGreaterOrEqualDefinition(ToscaTypes.INTEGER.toString(), "not an integer"), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatGreaterOrEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterOrEqualDefinition(ToscaTypes.FLOAT.toString(), 42.456f),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatGreaterOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterOrEqualDefinition(ToscaTypes.FLOAT.toString(), "not a float"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void invalidStringGreaterThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterThanDefinition(ToscaTypes.STRING.toString(), "value"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validIntegerGreaterThanConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterThanDefinition(ToscaTypes.INTEGER.toString(), 42),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerGreaterThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createGreaterThanDefinition(ToscaTypes.INTEGER.toString(), "not an integer"), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatGreaterThanConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterThanDefinition(ToscaTypes.FLOAT.toString(), 42.456f),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatGreaterThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createGreaterThanDefinition(ToscaTypes.FLOAT.toString(), "not a float"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void invalidStringInRangeConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createInRangeDefinition(ToscaTypes.STRING.toString(), "one", "two"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validIntegerInRangeConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createInRangeDefinition(ToscaTypes.INTEGER.toString(), 42, 65),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerInRangeConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createInRangeDefinition(ToscaTypes.INTEGER.toString(), 42.456f, 65.343f),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatInRangeConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createInRangeDefinition(ToscaTypes.FLOAT.toString(), 42.456f, 65.343f),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatInRangeConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createInRangeDefinition(ToscaTypes.FLOAT.toString(), 42.456f, "not a float"), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validStringLengthConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLenghtDefinition(ToscaTypes.STRING.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerLengthConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLenghtDefinition(ToscaTypes.INTEGER.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void invalidStringLessOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessOrEqualDefinition(ToscaTypes.STRING.toString(), "value"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validIntegerLessOrEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessOrEqualDefinition(ToscaTypes.INTEGER.toString(), 42),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerLessOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createLessOrEqualDefinition(ToscaTypes.INTEGER.toString(), "not an integer"), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatLessOrEqualConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessOrEqualDefinition(ToscaTypes.FLOAT.toString(), 42.456f),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatLessOrEqualConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessOrEqualDefinition(ToscaTypes.FLOAT.toString(), "not a float"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void invalidStringLessThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessDefinition(ToscaTypes.STRING.toString(), "value"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validIntegerLessThanConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessDefinition(ToscaTypes.INTEGER.toString(), 42),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerLessThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessDefinition(ToscaTypes.INTEGER.toString(), "not an integer"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatLessThanConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessDefinition(ToscaTypes.FLOAT.toString(), 42.456f),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatLessThanConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createLessDefinition(ToscaTypes.FLOAT.toString(), "not a float"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validStringMaxLengthConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createMaxLenghtDefinition(ToscaTypes.STRING.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerMaxLengthConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createMaxLenghtDefinition(ToscaTypes.INTEGER.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validStringMinLengthConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createMinLenghtDefinition(ToscaTypes.STRING.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerMinLengthConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createMinLenghtDefinition(ToscaTypes.INTEGER.toString(), 2),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validStringPatternConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createPatternDefinition(ToscaTypes.STRING.toString(), "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"), ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerPatternConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(createPatternDefinition(ToscaTypes.INTEGER.toString(), "2"),
                ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validStringValidValuesConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createValidValuesDefinition(ToscaTypes.STRING.toString(), Lists.newArrayList("value", "othervalue")), ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void validIntegerValidValuesConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createValidValuesDefinition(ToscaTypes.INTEGER.toString(), Lists.newArrayList("42", "56")), ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidIntegerValidValuesConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createValidValuesDefinition(ToscaTypes.INTEGER.toString(), Lists.newArrayList("42", "not an integer")), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }

    @Test
    public void validFloatValidValuesConstraintShouldNotCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createValidValuesDefinition(ToscaTypes.FLOAT.toString(), Lists.newArrayList("42.345", "56.652")), ToscaSequence.class);
        Assert.assertEquals(0, violations.size());
    }

    @Test
    public void invalidFloatValidValuesConstraintShouldCreateViolations() {
        Set<ConstraintViolation<PropertyDefinition>> violations = validator.validate(
                createValidValuesDefinition(ToscaTypes.FLOAT.toString(), Lists.newArrayList("42.345", "not a float")), ToscaSequence.class);
        Assert.assertEquals(2, violations.size());
    }
}