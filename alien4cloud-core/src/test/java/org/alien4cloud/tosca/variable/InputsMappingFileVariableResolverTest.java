package org.alien4cloud.tosca.variable;

import static org.alien4cloud.tosca.variable.InputsMappingFileVariableResolver.InputsMappingFileVariableResolverConfigured;
import static org.alien4cloud.tosca.variable.InputsMappingFileVariableResolver.configure;
import static org.alien4cloud.tosca.variable.PropertyDefinitionUtils.buildPropDef;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.alien4cloud.tosca.utils.PropertiesYamlParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import alien4cloud.model.application.Application;

public class InputsMappingFileVariableResolverTest {

    private InputsMappingFileVariableResolverConfigured inputsMappingFileVariableResolverConfigured;

    private Map<String, PropertyDefinition> inputsPropertyDefinitions;

    @Before
    public void setUp() throws Exception {
        inputsPropertyDefinitions = Maps.newHashMap();
        inputsPropertyDefinitions.put("int_input", buildPropDef(ToscaTypes.INTEGER));
        inputsPropertyDefinitions.put("float_input", buildPropDef(ToscaTypes.FLOAT));
        inputsPropertyDefinitions.put("string_input", buildPropDef(ToscaTypes.STRING));
        inputsPropertyDefinitions.put("complex_input", buildPropDef("datatype.complex_input_entry"));

        Resource yamlApp = new FileSystemResource("src/test/resources/alien/variables/variables_app_test.yml");
        Resource yamlEnv = new FileSystemResource("src/test/resources/alien/variables/variables_env_test.yml");

        AlienContextVariables alienContextVariables = new AlienContextVariables();
        Application application = new Application();
        application.setName("originalAppName");
        alienContextVariables.setApplication(application);

        inputsMappingFileVariableResolverConfigured = configure(PropertiesYamlParser.ToProperties.from(yamlApp),
                PropertiesYamlParser.ToProperties.from(yamlEnv), alienContextVariables);
    }

    @Test
    public void should_list_all_missing_variables() throws Exception {
        Resource yamlApp = new FileSystemResource("src/test/resources/alien/variables/variables_app_missing_var.yml");
        inputsMappingFileVariableResolverConfigured = configure(PropertiesYamlParser.ToProperties.from(yamlApp),
                PropertiesYamlParser.ToProperties.from(yamlApp), new AlienContextVariables());

        Resource inputsMapping = new FileSystemResource("src/test/resources/alien/variables/inputs_mapping_with_missing_variable.yml");

        try {
            inputsMappingFileVariableResolverConfigured.resolve(PropertiesYamlParser.ToMap.from(inputsMapping), inputsPropertyDefinitions);
            fail("should throw a MissingVariablesException when variables are missing");
        } catch (MissingVariablesException e) {
            assertThat(e.getMissingVariables()).hasSize(4);
            assertThat(e.getMissingVariables()).contains("missing_inner_variable", "missing_float_variable", "missing_string_variable", "missing_int_variable");
        }
    }

    @Test
    public void should_comply_with_property_definition_even_if_does_not_match_reality() throws Exception {
        // Given: a simplified inputs definition
        inputsPropertyDefinitions.put("complex_input", buildPropDef(ToscaTypes.MAP, ToscaTypes.STRING));

        // When
        Map<String, PropertyValue> inputsMappingFileResolved = resolve("src/test/resources/alien/variables/inputs_mapping_without_variable.yml");

        // Then: match the simplified inputs definition
        assertThat(inputsMappingFileResolved.get("complex_input")).isInstanceOf(ComplexPropertyValue.class);
        assertThat(inputsMappingFileResolved.get("complex_input")).isEqualTo(
                new ComplexPropertyValue(ImmutableMap.of("sub1", new ScalarPropertyValue(ImmutableMap.of("subfield11", "11", "subfield12", "12").toString()),
                        "sub2", new ScalarPropertyValue(ImmutableMap.of("subfield21", "21").toString()), "field01", new ScalarPropertyValue("01"))));
    }

    @Test
    public void check_inputs_mapping_can_be_parsed_when_no_variable() throws Exception {
        inputsMappingFileVariableResolverConfigured.customConverter(new ToscaTypeConverter((concreteType, id) -> {
            if (id.equals("datatype.complex_input_entry")) {
                DataType dataType = new DataType();
                dataType.setDeriveFromSimpleType(false);
                dataType.setProperties(ImmutableMap.of( //
                        "sub1", buildPropDef(ToscaTypes.MAP, ToscaTypes.STRING), //
                        "sub2", buildPropDef(ToscaTypes.MAP, ToscaTypes.STRING), //
                        "field01", buildPropDef(ToscaTypes.STRING) //
                ) //
                );
                return dataType;
            }

            return null;
        }));

        Map<String, PropertyValue> inputsMappingFileResolved = resolve("src/test/resources/alien/variables/inputs_mapping_without_variable.yml");

        assertThat(inputsMappingFileResolved).containsOnlyKeys("int_input", "float_input", "string_input", "complex_input");
        assertThat(inputsMappingFileResolved.get("int_input").getValue()).isEqualTo("10");
        assertThat(inputsMappingFileResolved.get("int_input")).isInstanceOf(ScalarPropertyValue.class);
        assertThat(inputsMappingFileResolved.get("float_input").getValue()).isEqualTo("3.14");
        assertThat(inputsMappingFileResolved.get("float_input")).isInstanceOf(ScalarPropertyValue.class);
        assertThat(inputsMappingFileResolved.get("string_input").getValue()).isEqualTo("text");
        assertThat(inputsMappingFileResolved.get("string_input")).isInstanceOf(ScalarPropertyValue.class);
        assertThat(inputsMappingFileResolved.get("complex_input")).isInstanceOf(ComplexPropertyValue.class);

        assertThat(inputsMappingFileResolved.get("complex_input")).isEqualTo( //
                new ComplexPropertyValue(ImmutableMap.of( //
                        "sub1", new ComplexPropertyValue(ImmutableMap.of( //
                                "subfield11", new ScalarPropertyValue("11"), //
                                "subfield12", new ScalarPropertyValue("12"))), //
                        "sub2", new ComplexPropertyValue(ImmutableMap.of( //
                                "subfield21", new ScalarPropertyValue("21"))), //
                        "field01", new ScalarPropertyValue("01"))) //
        );
    }

    private Map<String, PropertyValue> resolve(String path) throws MissingVariablesException {
        return inputsMappingFileVariableResolverConfigured.resolve(PropertiesYamlParser.ToMap.from(new FileSystemResource(path)), inputsPropertyDefinitions);
    }

    @Test
    public void check_inputs_mapping_can_be_parsed_when_variable() throws Exception {
        inputsMappingFileVariableResolverConfigured.customConverter(new ToscaTypeConverter((concreteType, id) -> {
            if (id.equals("datatype.complex_input_entry.sub1")) {
                DataType dataType = new DataType();
                dataType.setDeriveFromSimpleType(false);
                dataType.setProperties(ImmutableMap.of( //
                        "complex", buildPropDef(ToscaTypes.MAP, ToscaTypes.STRING) //
                ) //
                );
                return dataType;
            }

            if (id.equals("datatype.complex_input_entry")) {
                DataType dataType = new DataType();
                dataType.setDeriveFromSimpleType(false);
                dataType.setProperties(ImmutableMap.of( //
                        "sub1", buildPropDef("datatype.complex_input_entry.sub1"), //
                        "sub2", buildPropDef(ToscaTypes.MAP, ToscaTypes.STRING), //
                        "field01", buildPropDef(ToscaTypes.STRING) //
                ) //
                );
                return dataType;
            }

            return null;
        }));

        Map<String, PropertyValue> inputsMappingFileResolved = resolve("src/test/resources/alien/variables/inputs_mapping_with_variables.yml");

        assertThat(inputsMappingFileResolved.get("int_input")).isEqualTo(new ScalarPropertyValue("1"));
        assertThat(inputsMappingFileResolved.get("float_input")).isEqualTo(new ScalarPropertyValue("3.14"));
        assertThat(inputsMappingFileResolved.get("string_input")).isEqualTo(new ScalarPropertyValue("text_3.14"));

        assertThat(inputsMappingFileResolved.get("complex_input")).isEqualTo( //
                new ComplexPropertyValue(ImmutableMap.of( //
                        "sub1", new ComplexPropertyValue(ImmutableMap.of( //
                                "complex", new ComplexPropertyValue(ImmutableMap.of("subfield", new ScalarPropertyValue("text"))))), //
                        "sub2", new ComplexPropertyValue(ImmutableMap.of( //
                                "subfield21", new ScalarPropertyValue("1"))), //
                        "field01", new ScalarPropertyValue("text"))) //
        );

    }

    @Test
    public void check_uber_input_can_be_parsed() throws Exception {
        inputsPropertyDefinitions.put("uber_input", buildPropDef("datatype.uber"));

        /*
         * 
         * uber_input:
         * simple_var:
         * int_field: ${int_variable}
         * float_field: ${float_variable}
         * string_field: ${string_variable}
         * concat_field: ${int_variable}${float_variable}
         * simple_static:
         * int_field: 51
         * float_field: 16.64
         * string_field: "leffe"
         * complex:
         * complex_with_list: ${complex_with_list}
         * int_list:
         * - ${int_variable}
         * - ${int_variable}
         * float_list:
         * - ${float_variable}
         * - ${float_variable}
         * mix_list:
         * - ${int_variable}
         * - ${float_variable}
         * - ${string_variable}
         * static_mix_list:
         * - "jenlain"
         * - 16.64
         * - "kwak"
         * complex_with_var_in_leaf: ${complex_with_var_in_leaf}
         * 
         */
        inputsMappingFileVariableResolverConfigured.customConverter(new ToscaTypeConverter((concreteType, id) -> {
            DataType dataType = null;
            switch (id) {
            case "datatype.uber":
                dataType = new DataType();
                dataType.setDeriveFromSimpleType(false);
                dataType.setProperties(ImmutableMap.of( //
                        "simple_var", buildPropDef("datatype.uber.simple_var"), //
                        "complex", buildPropDef("datatype.uber.complex"), //
                        "simple_static", buildPropDef("datatype.uber.simple_static"), //
                        "complex_with_var_in_leaf", buildPropDef("datatype.uber.complex_with_var_in_leaf") //
                ) //
                );
                break;

            case "datatype.uber.simple_var":
                dataType = new DataType();
                dataType.setDeriveFromSimpleType(false);
                dataType.setProperties(ImmutableMap.of( //
                        "int_field", buildPropDef(ToscaTypes.INTEGER), //
                        "float_field", buildPropDef(ToscaTypes.FLOAT), //
                        "string_field", buildPropDef(ToscaTypes.STRING), //
                        "concat_field", buildPropDef(ToscaTypes.STRING) //
                ) //
                );
                break;

            case "datatype.uber.simple_static":
                dataType = new DataType();
                dataType.setDeriveFromSimpleType(false);
                dataType.setProperties(ImmutableMap.of( //
                        "int_field", buildPropDef(ToscaTypes.INTEGER), //
                        "float_field", buildPropDef(ToscaTypes.FLOAT), //
                        "string_field", buildPropDef(ToscaTypes.STRING) //
                ) //
                );
                break;

            case "datatype.uber.complex":
                dataType = new DataType();
                dataType.setDeriveFromSimpleType(false);
                dataType.setProperties(
                        ImmutableMap.<String, PropertyDefinition> builder().put("complex_with_list", buildPropDef("datatype.uber.complex.complex_with_list")) //
                                .put("int_list", buildPropDef(ToscaTypes.LIST, ToscaTypes.FLOAT)) //
                                .put("float_list", buildPropDef(ToscaTypes.LIST, ToscaTypes.STRING)) //
                                .put("mix_list", buildPropDef(ToscaTypes.LIST, ToscaTypes.STRING)) //
                                .put("static_mix_list", buildPropDef(ToscaTypes.LIST, ToscaTypes.STRING)).build() //
                );
                break;

            case "datatype.uber.complex.complex_with_list":
                dataType = new DataType();
                dataType.setDeriveFromSimpleType(false);
                dataType.setProperties(ImmutableMap.<String, PropertyDefinition> builder().put("subfield1", buildPropDef(ToscaTypes.STRING)) // NEED TO BE
                                                                                                                                             // IMPROVED
                        .put("subfield2", buildPropDef(ToscaTypes.MAP, buildPropDef(ToscaTypes.LIST, ToscaTypes.STRING))).build() //
                );
                break;

            case "datatype.uber.complex_with_var_in_leaf":
                dataType = new DataType();
                dataType.setDeriveFromSimpleType(false);
                dataType.setProperties(ImmutableMap.of( //
                        "complex", buildPropDef(ToscaTypes.MAP, ToscaTypes.STRING)) //
                );
                break;
            }

            return dataType;
        }));

        Map<String, PropertyValue> inputsMappingFileResolved = resolve("src/test/resources/alien/variables/inputs_mapping_uber.yml");

        assertThat(inputsMappingFileResolved).containsOnlyKeys("uber_input");
        assertThat(inputsMappingFileResolved.get("uber_input")).isInstanceOf(ComplexPropertyValue.class);
        ComplexPropertyValue uberInput = (ComplexPropertyValue) inputsMappingFileResolved.get("uber_input");

        assertThat(sub(ScalarPropertyValue.class, uberInput, "simple_var", "string_field").getValue()).isEqualTo("text");
        assertThat(sub(ScalarPropertyValue.class, uberInput, "simple_var", "concat_field").getValue()).isEqualTo("13.14");
        assertThat(sub(ScalarPropertyValue.class, uberInput, "simple_static", "int_field").getValue()).isEqualTo("51");
        assertThat(sub(ScalarPropertyValue.class, uberInput, "simple_static", "float_field").getValue()).isEqualTo("16.64");
        assertThat(sub(ScalarPropertyValue.class, uberInput, "simple_static", "string_field").getValue()).isEqualTo("leffe");

        assertThat(sub(ListPropertyValue.class, uberInput, "complex", "int_list").getValue()).isEqualTo(Arrays.asList(new ScalarPropertyValue("1"), new ScalarPropertyValue("1")));
        assertThat(sub(ListPropertyValue.class, uberInput, "complex", "float_list").getValue()).hasSize(2);
        assertThat(sub(ListPropertyValue.class, uberInput, "complex", "mix_list").getValue()).hasSize(3);
        assertThat(sub(ListPropertyValue.class, uberInput, "complex", "static_mix_list").getValue()).hasSize(3);
        assertThat(sub(ListPropertyValue.class, uberInput, "complex", "complex_with_list", "subfield2", "sublist").getValue()).hasSize(3);

        assertThat(sub(ScalarPropertyValue.class, uberInput, "complex_with_var_in_leaf", "complex", "subfield").getValue()).isEqualTo("text");
    }

    private <T extends PropertyValue> T sub(Class<T> clazz, Object propertyValue, String... fieldNames) {
        Object subPropertyValue = propertyValue;
        for (String fieldName : fieldNames) {
            if (propertyValue instanceof ComplexPropertyValue) {
                subPropertyValue = ((ComplexPropertyValue) subPropertyValue).getValue().get(fieldName);
            } else {
                Assert.fail("Cannot find field <" + fieldNames + "> into " + propertyValue);
            }
        }

        T result = null;
        try {
            result = (T) subPropertyValue;
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Cannot cast <" + propertyValue + "> subfield <" + fieldNames + "> into " + clazz.getName());
        }
        return result;
    }

}