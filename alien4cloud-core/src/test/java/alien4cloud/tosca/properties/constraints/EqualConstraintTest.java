package alien4cloud.tosca.properties.constraints;

import org.junit.Test;

import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

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
        constraint.initialize(ToscaType.STRING_TYPE);
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
        constraint.initialize(ToscaType.STRING_TYPE);
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
        constraint.initialize(ToscaType.INTEGER_TYPE);
        constraint.validate(1l);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailInteger() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaType.INTEGER_TYPE);
        constraint.validate(2l);
    }

    @Test
    public void testEqualConstraintSatisfiedFloat() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaType.FLOAT_TYPE);
        constraint.validate(1.0d);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailFloat() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaType.FLOAT_TYPE);
        constraint.validate(2.0d);
    }

    @Test
    public void testEqualConstraintSatisfiedTime() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException, InvalidPropertyValueException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1 m");
        constraint.initialize(ToscaType.TIME_TYPE);
        constraint.validate(ToscaType.TIME_TYPE.parse("1 m"));
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailTime() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException,
            InvalidPropertyValueException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1 m");
        constraint.initialize(ToscaType.TIME_TYPE);
        constraint.validate(ToscaType.TIME_TYPE.parse("2 d"));
    }

    // TODO test with version

    // TODO test with timestamp
}