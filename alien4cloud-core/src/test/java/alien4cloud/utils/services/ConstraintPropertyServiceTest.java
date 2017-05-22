package alien4cloud.utils.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import alien4cloud.exception.InvalidArgumentException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.constraints.LengthConstraint;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Maps;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.tosca.context.ToscaContext;

public class ConstraintPropertyServiceTest {
    // valid value tests

    @Test
    public void testValidStringProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.STRING);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "value", propertyDefinition);
    }

    @Test
    public void testValidIntegerProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.INTEGER);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "128", propertyDefinition);
    }

    @Test
    public void testValidFloatProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.FLOAT);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "128", propertyDefinition);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "128.34", propertyDefinition);
    }

    @Test
    public void testValidBooleanProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.BOOLEAN);
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
        propertyDefinition.setType(ToscaTypes.TIMESTAMP);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "2015-01-15 00:00:00", propertyDefinition);
    }

    @Test
    public void testValidVersionProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.VERSION);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "2.0", propertyDefinition);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "1.0.0-SNAPSHOT", propertyDefinition);
    }

    @Test
    public void testValidListProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.LIST);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        propertyDefinition.setEntrySchema(entrySchema);
        Object propertyValue = ImmutableList.builder().add("aa", "bb").build();
        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);


        // test length constraint
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(2);
        List<PropertyConstraint> constraints = Lists.newArrayList();
        constraints.add(lengthConstraint);
        propertyDefinition.setConstraints(constraints);
        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
    }

    @Test
    public void testValidMapProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.MAP);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        propertyDefinition.setEntrySchema(entrySchema);
        Object propertyValue = ImmutableMap.builder().put("aa", "bb").build();
        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);

        // test length constraint
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(1);
        List<PropertyConstraint> constraints = Lists.newArrayList();
        constraints.add(lengthConstraint);
        propertyDefinition.setConstraints(constraints);
        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
    }

    // invalid value tests

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidIntegerProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.INTEGER);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "aaaa128", propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidFloatProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.FLOAT);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "aaaa128", propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidVersionProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.VERSION);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "anything", propertyDefinition);
    }

    // constraint test
    @Test
    public void testValidStringConstraintProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.STRING);
        propertyDefinition.setConstraints(new ArrayList<PropertyConstraint>());
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(3);
        propertyDefinition.getConstraints().add(lengthConstraint);

        ConstraintPropertyService.checkSimplePropertyConstraint("test", "val", propertyDefinition);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidStringConstraintProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.STRING);
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
        propertyDefinition.setType(ToscaTypes.FLOAT);
        ConstraintPropertyService.checkSimplePropertyConstraint("test", "aaa", propertyDefinition);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidListProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.LIST);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        propertyDefinition.setEntrySchema(entrySchema);
        Object propertyValue = ImmutableMap.builder().put("aa", "bb").build();
        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
    }

    @Test(expected = InvalidArgumentException.class)
    public void testInvalidMapProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.MAP);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        propertyDefinition.setEntrySchema(entrySchema);
        Object propertyValue = ImmutableList.builder().add("aa", "bb").build();
        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
    }

    /////////////////////////////////////////////////////////////////
    // Tests on complex properties validation
    /////////////////////////////////////////////////////////////////

    @Test
    public void testMapPropertyInComplex() throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        // given
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("alien.test.ComplexStruct");

        PropertyDefinition subPropertyDefinition = new PropertyDefinition();
        subPropertyDefinition.setType(ToscaTypes.MAP);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        subPropertyDefinition.setEntrySchema(entrySchema);

        DataType dataType = new DataType();
        dataType.setProperties(Maps.newHashMap());
        dataType.getProperties().put("myMap", subPropertyDefinition);
        dataType.setElementId("alien.test.ComplexStruct");

        ToscaContext.init(Collections.emptySet());
        ICSARRepositorySearchService mockSearchService = Mockito.mock(ICSARRepositorySearchService.class);
        Mockito.when(mockSearchService.getRequiredElementInDependencies(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dataType);
        Mockito.when(mockSearchService.getElementInDependencies(Mockito.any(), Mockito.any(), Mockito.anySet())).thenReturn(dataType);
        ToscaContext.setCsarRepositorySearchService(mockSearchService);

        // when
        Object subPropertyValue = ImmutableMap.builder().put("aa", "bb").build();
        Object propertyValue = ImmutableMap.builder().put("myMap", subPropertyValue).build();

        // then
        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidMapPropertyInComplex() throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        // given
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("alien.test.ComplexStruct");

        PropertyDefinition subPropertyDefinition = new PropertyDefinition();
        subPropertyDefinition.setType(ToscaTypes.MAP);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        subPropertyDefinition.setEntrySchema(entrySchema);

        DataType dataType = new DataType();
        dataType.setProperties(Maps.newHashMap());
        dataType.getProperties().put("myMap", subPropertyDefinition);
        dataType.setElementId("alien.test.ComplexStruct");

        ToscaContext.init(Collections.emptySet());
        ICSARRepositorySearchService mockSearchService = Mockito.mock(ICSARRepositorySearchService.class);
        Mockito.when(mockSearchService.getRequiredElementInDependencies(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dataType);
        Mockito.when(mockSearchService.getElementInDependencies(Mockito.any(), Mockito.any(), Mockito.anySet())).thenReturn(dataType);
        ToscaContext.setCsarRepositorySearchService(mockSearchService);

        // when
        Object propertyValue = ImmutableMap.builder().put("myMap", "aa").build();

        // then -> ConstraintViolationException
        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
    }
}
