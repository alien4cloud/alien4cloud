package alien4cloud.utils.services;

import java.util.ArrayList;

import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.constraints.LengthConstraint;
import org.junit.Test;

public class ConstraintPropertyServiceTest {
    // valid value tests

    @Test
    public void testValidStringProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("string");
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "value", propertyDefinition);
    }

    @Test
    public void testValidIntegerProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("integer");
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "128", propertyDefinition);
    }

    @Test
    public void testValidFloatProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("float");
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "128", propertyDefinition);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "128.34", propertyDefinition);
    }

    @Test
    public void testValidBooleanProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("boolean");
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "true", propertyDefinition);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "false", propertyDefinition);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "1", propertyDefinition);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "0", propertyDefinition);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "TRUE", propertyDefinition);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "FALSE", propertyDefinition);
        // in fact anything can be used for boolean
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "anything", propertyDefinition);
    }

    @Test
    public void testValidTimestampProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("timestamp");
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "2015-01-15 00:00:00", propertyDefinition);
    }

    @Test
    public void testValidVersionProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("version");
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "2.0", propertyDefinition);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "1.0.0-SNAPSHOT", propertyDefinition);
    }

    // invalid value tests

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidIntegerProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("integer");
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "aaaa128", propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidFloatProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("float");
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "aaaa128", propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidVersionProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("version");
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "anything", propertyDefinition);
    }

    // constraint test
    @Test
    public void testValidStringConstraintProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("string");
        propertyDefinition.setConstraints(new ArrayList<PropertyConstraint>());
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(3);
        propertyDefinition.getConstraints().add(lengthConstraint);

        ConstraintPropertyService.checkSimplePropertyConstraint("test", "val", propertyDefinition);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidStringConstraintProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("string");
        propertyDefinition.setConstraints(new ArrayList<PropertyConstraint>());
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(3);
        propertyDefinition.getConstraints().add(lengthConstraint);

        ConstraintPropertyService.checkSimplePropertyConstraint("test", "value", propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidFloatPropertyWithConstraint() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setConstraints(new ArrayList<PropertyConstraint>());
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(3);
        propertyDefinition.getConstraints().add(lengthConstraint);
        propertyDefinition.setType("float");
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "aaa", propertyDefinition);
    }
}
