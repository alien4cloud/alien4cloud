package org.alien4cloud.tosca.model.definitions.constraints;

import org.junit.Test;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;

public class EqualConstraintTest {
    @Test
    public void testEqualConstraintSatisfiedNull() throws ConstraintViolationException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual(null);
        constraint.validate(null);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailNullValue() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("value");
        constraint.initialize(ToscaTypes.STRING_TYPE);
        constraint.validate(null);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailNullProperty() throws ConstraintViolationException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual(null);
        constraint.validate("value");
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailNullDef() throws ConstraintViolationException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual(null);
        constraint.validate("value");
    }

    @Test
    public void testEqualConstraintSatisfiedString() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("value");
        constraint.initialize(ToscaTypes.STRING_TYPE);
        constraint.validate("value");
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailString() throws ConstraintViolationException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("value");
        constraint.validate("othervalue");
    }

    @Test
    public void testEqualConstraintSatisfiedInteger() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaTypes.INTEGER_TYPE);
        constraint.validate(1l);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailInteger() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaTypes.INTEGER_TYPE);
        constraint.validate(2l);
    }

    @Test
    public void testEqualConstraintSatisfiedFloat() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaTypes.FLOAT_TYPE);
        constraint.validate(1.0d);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailFloat() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaTypes.FLOAT_TYPE);
        constraint.validate(2.0d);
    }

    @Test
    public void testEqualConstraintSatisfiedTime() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException, InvalidPropertyValueException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1 m");
        constraint.initialize(ToscaTypes.TIME_TYPE);
        constraint.validate(ToscaTypes.TIME_TYPE.parse("1 m"));
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailTime() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException,
            InvalidPropertyValueException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1 m");
        constraint.initialize(ToscaTypes.TIME_TYPE);
        constraint.validate(ToscaTypes.TIME_TYPE.parse("2 d"));
    }

    // TODO test with version

    // TODO test with timestamp
}