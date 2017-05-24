package alien4cloud.utils.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.constraints.LengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MaxLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MinLengthConstraint;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.tosca.context.ToscaContext;

public class ConstraintPropertyServiceTest {
    // valid value tests

    @Test
    public void testValidStringProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.STRING);
        ConstraintPropertyService.checkPropertyConstraint("test", "value", propertyDefinition);
    }

    @Test
    public void testValidIntegerProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.INTEGER);
        ConstraintPropertyService.checkPropertyConstraint("test", "128", propertyDefinition);
    }

    @Test
    public void testValidFloatProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.FLOAT);
        ConstraintPropertyService.checkPropertyConstraint("test", "128", propertyDefinition);
        ConstraintPropertyService.checkPropertyConstraint("test", "128.34", propertyDefinition);
    }

    @Test
    public void testValidBooleanProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.BOOLEAN);
        ConstraintPropertyService.checkPropertyConstraint("test", "true", propertyDefinition);
        ConstraintPropertyService.checkPropertyConstraint("test", "false", propertyDefinition);
        ConstraintPropertyService.checkPropertyConstraint("test", "1", propertyDefinition);
        ConstraintPropertyService.checkPropertyConstraint("test", "0", propertyDefinition);
        ConstraintPropertyService.checkPropertyConstraint("test", "TRUE", propertyDefinition);
        ConstraintPropertyService.checkPropertyConstraint("test", "FALSE", propertyDefinition);
        // in fact anything can be used for boolean
        ConstraintPropertyService.checkPropertyConstraint("test", "anything", propertyDefinition);
    }

    @Test
    public void testValidTimestampProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.TIMESTAMP);
        ConstraintPropertyService.checkPropertyConstraint("test", "2001-12-15T02:59:43.1Z", propertyDefinition);
    }

    @Test
    public void testValidVersionProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.VERSION);
        ConstraintPropertyService.checkPropertyConstraint("test", "2.0", propertyDefinition);
        ConstraintPropertyService.checkPropertyConstraint("test", "1.0.0-SNAPSHOT", propertyDefinition);
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

        // test min_length constraint
        MinLengthConstraint minLengthConstraint = new MinLengthConstraint();
        minLengthConstraint.setMinLength(1);
        constraints.add(minLengthConstraint);
        propertyDefinition.setConstraints(constraints);
        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);

        // test max_length constraint
        MaxLengthConstraint maxLengthConstraint = new MaxLengthConstraint();
        maxLengthConstraint.setMaxLength(3);
        constraints.add(maxLengthConstraint);
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
        ConstraintPropertyService.checkPropertyConstraint("test", "aaaa128", propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidFloatProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.FLOAT);
        ConstraintPropertyService.checkPropertyConstraint("test", "aaaa128", propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidVersionProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.VERSION);
        ConstraintPropertyService.checkPropertyConstraint("test", "anything", propertyDefinition);
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

        ConstraintPropertyService.checkPropertyConstraint("test", "val", propertyDefinition);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidStringConstraintProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.STRING);
        propertyDefinition.setConstraints(new ArrayList<PropertyConstraint>());
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(3);
        propertyDefinition.getConstraints().add(lengthConstraint);

        ConstraintPropertyService.checkPropertyConstraint("test", "value", propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidFloatPropertyWithConstraint() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setConstraints(new ArrayList<PropertyConstraint>());
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(3);
        propertyDefinition.getConstraints().add(lengthConstraint);
        propertyDefinition.setType(ToscaTypes.FLOAT);
        ConstraintPropertyService.checkPropertyConstraint("test", "aaa", propertyDefinition);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidListProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.LIST);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        propertyDefinition.setEntrySchema(entrySchema);
        Object propertyValue = ImmutableMap.builder().put("aa", "bb").build();
        ToscaContext.init(new HashSet<>());
        ICSARRepositorySearchService originalCsarRepositorySearchService = ToscaContext.getCsarRepositorySearchService();
        ICSARRepositorySearchService mockSearchService = Mockito.mock(ICSARRepositorySearchService.class);
        try {
            ToscaContext.setCsarRepositorySearchService(mockSearchService);
            ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
        } finally {
            ToscaContext.setCsarRepositorySearchService(originalCsarRepositorySearchService);
            ToscaContext.destroy();
        }
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidMapProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.MAP);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        propertyDefinition.setEntrySchema(entrySchema);
        Object propertyValue = ImmutableList.builder().add("aa", "bb").build();
        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidMapLengthConstraintProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.MAP);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        propertyDefinition.setEntrySchema(entrySchema);
        Object propertyValue = ImmutableMap.builder().put("aa", "bb").build();

        // invalid length constraint
        LengthConstraint lengthConstraint = new LengthConstraint();
        lengthConstraint.setLength(4);
        List<PropertyConstraint> constraints = Lists.newArrayList();
        constraints.add(lengthConstraint);
        propertyDefinition.setConstraints(constraints);

        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testInvalidMapMinLengthConstraintProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.MAP);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        propertyDefinition.setEntrySchema(entrySchema);
        Object propertyValue = ImmutableMap.builder().put("aa", "bb").build();

        // invalid length constraint
        MinLengthConstraint lengthConstraint = new MinLengthConstraint();
        lengthConstraint.setMinLength(4);
        List<PropertyConstraint> constraints = Lists.newArrayList();
        constraints.add(lengthConstraint);
        propertyDefinition.setConstraints(constraints);

        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
    }

    @Test(expected = ConstraintValueDoNotMatchPropertyTypeException.class)
    public void testInvalidConstraintProperty() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.MAP);
        PropertyDefinition entrySchema = new PropertyDefinition();
        entrySchema.setType(ToscaTypes.STRING);
        propertyDefinition.setEntrySchema(entrySchema);
        Object propertyValue = ImmutableMap.builder().put("aa", "bb").build();

        // invalid length constraint
        LessOrEqualConstraint lengthConstraint = new LessOrEqualConstraint();
        lengthConstraint.setLessOrEqual("aa");
        List<PropertyConstraint> constraints = Lists.newArrayList();
        constraints.add(lengthConstraint);
        propertyDefinition.setConstraints(constraints);

        ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
    }

    // ///////////////////////////////////////////////////////////////
    // Tests on complex properties validation
    // ///////////////////////////////////////////////////////////////

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

        ICSARRepositorySearchService originalCsarRepositorySearchService = ToscaContext.getCsarRepositorySearchService();
        ToscaContext.init(new HashSet<>());
        ICSARRepositorySearchService mockSearchService = Mockito.mock(ICSARRepositorySearchService.class);
        Mockito.when(mockSearchService.getRequiredElementInDependencies(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dataType);
        Mockito.when(mockSearchService.getElementInDependencies(Mockito.any(), Mockito.any(), Mockito.anySet())).thenReturn(dataType);
        try {
            ToscaContext.setCsarRepositorySearchService(mockSearchService);
            // when
            Object subPropertyValue = ImmutableMap.builder().put("aa", "bb").build();
            Object propertyValue = ImmutableMap.builder().put("myMap", subPropertyValue).build();
            // then
            ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
        } finally {
            ToscaContext.setCsarRepositorySearchService(originalCsarRepositorySearchService);
            ToscaContext.destroy();
        }
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
        ICSARRepositorySearchService originalCsarRepositorySearchService = ToscaContext.getCsarRepositorySearchService();
        ToscaContext.init(new HashSet<>());
        ICSARRepositorySearchService mockSearchService = Mockito.mock(ICSARRepositorySearchService.class);
        Mockito.when(mockSearchService.getRequiredElementInDependencies(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dataType);
        Mockito.when(mockSearchService.getElementInDependencies(Mockito.any(), Mockito.any(), Mockito.anySet())).thenReturn(dataType);
        try {
            ToscaContext.setCsarRepositorySearchService(mockSearchService);
            // when
            Object propertyValue = ImmutableMap.builder().put("myMap", "aa").build();
            // then -> ConstraintViolationException
            ConstraintPropertyService.checkPropertyConstraint("test", propertyValue, propertyDefinition);
        } finally {
            ToscaContext.setCsarRepositorySearchService(originalCsarRepositorySearchService);
            ToscaContext.destroy();
        }
    }
}
