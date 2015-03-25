package alien4cloud.utils.services;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.constraints.LengthConstraint;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

public class ConstraintPropertyServiceTest {

    private ConstraintPropertyService constraintPropertyService;
    
    @Before
    public void prepare() {
        constraintPropertyService = new ConstraintPropertyService();
    }
    
    // valid value tests

    @Test
    public void testValidStringProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("string");
        constraintPropertyService.checkPropertyConstraint("test", "value", propertyDefinition);
    }

    @Test
    public void testValidIntegerProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("integer");
        constraintPropertyService.checkPropertyConstraint("test", "128", propertyDefinition);
    }

    @Test
    public void testValidFloatProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("float");
        constraintPropertyService.checkPropertyConstraint("test", "128", propertyDefinition);
        constraintPropertyService.checkPropertyConstraint("test", "128.34", propertyDefinition);
    }

    @Test
    public void testValidBooleanProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("boolean");
        constraintPropertyService.checkPropertyConstraint("test", "true", propertyDefinition);
        constraintPropertyService.checkPropertyConstraint("test", "false", propertyDefinition);
        constraintPropertyService.checkPropertyConstraint("test", "1", propertyDefinition);
        constraintPropertyService.checkPropertyConstraint("test", "0", propertyDefinition);
        constraintPropertyService.checkPropertyConstraint("test", "TRUE", propertyDefinition);
        constraintPropertyService.checkPropertyConstraint("test", "FALSE", propertyDefinition);
        // in fact anything can be used for boolean
        constraintPropertyService.checkPropertyConstraint("test", "anything", propertyDefinition);
    }

    @Test
    public void testValidTimestampProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("timestamp");
        constraintPropertyService.checkPropertyConstraint("test", "2015-01-15 00:00:00", propertyDefinition);
    }

    @Test
    public void testValidVersionProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("version");
        constraintPropertyService.checkPropertyConstraint("test", "2.0", propertyDefinition);
        constraintPropertyService.checkPropertyConstraint("test", "1.0.0-SNAPSHOT", propertyDefinition);
    }

    // invalid value tests

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidIntegerProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("integer");
        constraintPropertyService.checkPropertyConstraint("test", "aaaa128", propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidFloatProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("float");
        constraintPropertyService.checkPropertyConstraint("test", "aaaa128", propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidVersionProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("version");
        constraintPropertyService.checkPropertyConstraint("test", "anything", propertyDefinition);
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

        constraintPropertyService.checkPropertyConstraint("test", "val", propertyDefinition);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidStringConstraintProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("string");
        propertyDefinition.setConstraints(new ArrayList<PropertyConstraint>());
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(3);
        propertyDefinition.getConstraints().add(lengthConstraint);

        constraintPropertyService.checkPropertyConstraint("test", "value", propertyDefinition);
    }

}
