package org.alien4cloud.tosca.variable;

import alien4cloud.utils.version.Version;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.normative.primitives.Time;
import org.alien4cloud.tosca.normative.primitives.TimeUnit;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.alien4cloud.tosca.variable.PropertyDefinitionUtils.buildPropDef;
import static org.assertj.core.api.Assertions.assertThat;

public class ToscaTypeConverterTest {

    private ToscaTypeConverter converter;

    @SneakyThrows
    private static DataType findDataType(Class<? extends DataType> concreteType, String id) {
        switch (id) {
            case "alien.nodes.test.ComplexDataType":
                DataType dataType = new DataType();
                Map<String, PropertyDefinition> propertyDefinitionMap = Maps.newHashMap();
                new PropertyDefinition();
                propertyDefinitionMap.put("nested", buildPropDef("string"));
                propertyDefinitionMap.put("nested_array", buildPropDef("list", "string"));
                propertyDefinitionMap.put("nested_map", buildPropDef("map", "string"));
                dataType.setProperties(propertyDefinitionMap);
                dataType.setElementId("alien.nodes.test.ComplexDataType");
                return dataType;

            default:
                return null;
        }
    }

    @Before
    public void setUp() throws Exception {
        converter = new ToscaTypeConverter(ToscaTypeConverterTest::findDataType);
    }

    @Test
    public void convert_time_to_property_value() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.TIME);

        PropertyValue propertyValue = converter.toPropertyValue("2 d", propertyDefinition);

        Object time = ToscaTypes.fromYamlTypeName(propertyDefinition.getType()).parse(propertyValue.getValue().toString());
        assertThat(time).isInstanceOf(Time.class);
        assertThat(time).isEqualTo(new Time(2, TimeUnit.D));
    }

    @Test
    public void convert_version_to_property_value() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.VERSION);

        PropertyValue propertyValue = converter.toPropertyValue("3.4-SNAPSHOT", propertyDefinition);

        Object version = ToscaTypes.fromYamlTypeName(propertyDefinition.getType()).parse(propertyValue.getValue().toString());
        assertThat(version).isInstanceOf(Version.class);
        assertThat(version).isEqualTo(new Version("3.4-SNAPSHOT"));
    }

    @Test
    public void convert_complex_data_type_to_property_value() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("alien.nodes.test.ComplexDataType");

        PropertyValue propertyValue = converter.toPropertyValue(
                ImmutableMap.of(
                        "nested", "nested value",
                        "nested_array", Arrays.asList("item1", "item2", "item3"),
                        "nested_map", ImmutableMap.of("key1", "value1", "key2", "value2")
                ), propertyDefinition);

        assertThat(propertyValue).isInstanceOf(ComplexPropertyValue.class);
        ComplexPropertyValue complexPropertyValue = (ComplexPropertyValue) propertyValue;
        assertThat(complexPropertyValue.getValue().get("nested_map")).isEqualTo(ImmutableMap.of("key1", "value1", "key2", "value2"));
        assertThat(complexPropertyValue.getValue().get("nested_array")).isEqualTo(Arrays.asList("item1", "item2", "item3"));
        assertThat(complexPropertyValue.getValue().get("nested")).isEqualTo("nested value");
    }


    @Test
    public void convert_version() throws Exception {
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(ToscaTypes.VERSION);

        Object value = converter.toPropertyValue("3.4-SNAPSHOT", propertyDefinition);

        assertThat(value).isInstanceOf(ScalarPropertyValue.class);
        assertThat(value).isEqualTo(new ScalarPropertyValue("3.4-SNAPSHOT"));
    }
}