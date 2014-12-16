package alien4cloud.tosca.properties.constraints;

import org.junit.Test;

import alien4cloud.tosca.model.ToscaType;
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
        constraint.initialize(ToscaType.STRING);
        constraint.validate(null);
    }
    
    @Test
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
        constraint.initialize(ToscaType.STRING);
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
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(1l);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailInteger() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaType.INTEGER);
        constraint.validate(2l);
    }

    @Test
    public void testEqualConstraintSatisfiedFloat() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaType.FLOAT);
        constraint.validate(1.0d);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testEqualConstraintFailFloat() throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        EqualConstraint constraint = new EqualConstraint();
        constraint.setEqual("1");
        constraint.initialize(ToscaType.FLOAT);
        constraint.validate(2.0d);
    }

    // TODO test with version

    // TODO test with timestamp
}