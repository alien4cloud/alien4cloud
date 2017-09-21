package org.alien4cloud.tosca.variable;

import alien4cloud.model.application.Application;
import alien4cloud.utils.MapUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.alien4cloud.tosca.utils.PropertiesYamlParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.Collection;
import java.util.Map;

import static org.alien4cloud.tosca.variable.InputsMappingFileVariableResolver.InputsMappingFileVariableResolverConfigured;
import static org.alien4cloud.tosca.variable.InputsMappingFileVariableResolver.configure;
import static org.alien4cloud.tosca.variable.PropertyDefinitionUtils.buildPropDef;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class InputsMappingFileVariableResolverTest {

    private InputsMappingFileVariableResolverConfigured inputsMappingFileVariableResolverConfigured;

    private Map<String, PropertyDefinition> inputsPropertyDefinitions;

    @Before
    public void setUp() throws Exception {
        inputsPropertyDefinitions = Maps.newHashMap();
        inputsPropertyDefinitions.put("int_input", buildPropDef(ToscaTypes.INTEGER));
        inputsPropertyDefinitions.put("float_input", buildPropDef(ToscaTypes.FLOAT));
        inputsPropertyDefinitions.put("string_input", buildPropDef(ToscaTypes.STRING));
        inputsPropertyDefinitions.put("complex_input", buildPropDef(ToscaTypes.MAP, ToscaTypes.STRING));
        inputsPropertyDefinitions.put("uber_input", buildPropDef(ToscaTypes.MAP, ToscaTypes.STRING));

        Resource yamlApp = new FileSystemResource("src/test/resources/alien/variables/variables_app_test.yml");
        Resource yamlEnv = new FileSystemResource("src/test/resources/alien/variables/variables_env_test.yml");

        AlienContextVariables alienContextVariables = new AlienContextVariables();
        Application application = new Application();
        application.setName("originalAppName");
        alienContextVariables.setApplication(application);

        inputsMappingFileVariableResolverConfigured = configure(
                PropertiesYamlParser.ToProperties.from(yamlApp),
                PropertiesYamlParser.ToProperties.from(yamlEnv),
                alienContextVariables);
    }

    @Test
    public void should_list_all_missing_variables() throws Exception {
        Resource yamlApp = new FileSystemResource("src/test/resources/alien/variables/variables_app_missing_var.yml");
        inputsMappingFileVariableResolverConfigured = configure(
                PropertiesYamlParser.ToProperties.from(yamlApp),
                PropertiesYamlParser.ToProperties.from(yamlApp),
                new AlienContextVariables()
        );

        Resource inputsMapping = new FileSystemResource("src/test/resources/alien/variables/inputs_mapping_with_missing_variable.yml");

        try {
            inputsMappingFileVariableResolverConfigured.resolve(PropertiesYamlParser.ToMap.from(inputsMapping), inputsPropertyDefinitions);
            fail("should throw a MissingVariablesException when variables are missing");
        } catch (MissingVariablesException e) {
            assertThat(e.getMissingVariables()).hasSize(4);
            assertThat(e.getMissingVariables()).contains(
                    "missing_inner_variable",
                    "missing_float_variable",
                    "missing_string_variable",
                    "missing_int_variable");
        }
    }

    @Test
    public void check_inputs_mapping_can_be_parsed_when_no_variable() throws Exception {
        Resource inputsMapping = new FileSystemResource("src/test/resources/alien/variables/inputs_mapping_without_variable.yml");
        Map<String, Object> inputsMappingAsProperties = PropertiesYamlParser.ToMap.from(inputsMapping);
        Map<String, PropertyValue> inputsMappingFileResolved = inputsMappingFileVariableResolverConfigured.resolve(inputsMappingAsProperties, inputsPropertyDefinitions);

        assertThat(inputsMappingFileResolved).containsOnlyKeys(Iterables.toArray(inputsMappingAsProperties.keySet(), String.class));
        assertThat(inputsMappingFileResolved.get("int_input").getValue()).isEqualTo("10");
        assertThat(inputsMappingFileResolved.get("int_input")).isInstanceOf(ScalarPropertyValue.class);
        assertThat(inputsMappingFileResolved.get("float_input").getValue()).isEqualTo("3.14");
        assertThat(inputsMappingFileResolved.get("float_input")).isInstanceOf(ScalarPropertyValue.class);
        assertThat(inputsMappingFileResolved.get("string_input").getValue()).isEqualTo("text");
        assertThat(inputsMappingFileResolved.get("string_input")).isInstanceOf(ScalarPropertyValue.class);
        assertThat(inputsMappingFileResolved.get("complex_input")).isInstanceOf(ComplexPropertyValue.class);

        // this result may seems weird but the current definition is does not match exactly the object (entry definition is String)
        assertThat(inputsMappingFileResolved.get("complex_input")).isEqualTo(
                new ComplexPropertyValue(ImmutableMap.of(
                        "sub1", new ScalarPropertyValue(ImmutableMap.of("subfield11", "11", "subfield12", "12").toString()),
                        "sub2", new ScalarPropertyValue(ImmutableMap.of("subfield21", "21").toString()),
                        "field01", new ScalarPropertyValue("01"))
                )
        );
    }

    @Test
    @Ignore
    public void check_inputs_mapping_can_be_parsed_when_variable() throws Exception {
        Resource inputsMapping = new FileSystemResource("src/test/resources/alien/variables/inputs_mapping_with_variables.yml");
        Map<String, Object> inputsMappingAsProperties = PropertiesYamlParser.ToMap.from(inputsMapping);
        Map<String, PropertyValue> inputsMappingFileResolved = inputsMappingFileVariableResolverConfigured.resolve(inputsMappingAsProperties, inputsPropertyDefinitions);

        assertThat(inputsMappingFileResolved).containsOnlyKeys(Iterables.toArray(inputsMappingAsProperties.keySet(), String.class));
        assertThat(inputsMappingFileResolved.get("int_input")).isEqualTo(new ScalarPropertyValue("1"));
        assertThat(inputsMappingFileResolved.get("float_input")).isEqualTo(new ScalarPropertyValue("3.14"));
        assertThat(inputsMappingFileResolved.get("string_input")).isEqualTo(new ScalarPropertyValue("text_3.14"));
        assertThat(inputsMappingFileResolved.get("complex_input")).isEqualTo(new ComplexPropertyValue(
                ImmutableMap.of(
                        "sub1", ImmutableMap.of("complex", ImmutableMap.of("subfield", "text")),
                        "sub2", ImmutableMap.of("subfield21", "1"),
                        "field01", "text")
        ));
    }

    @Ignore
    @Test
    public void check_uber_input_can_be_parsed() throws Exception {
        Resource inputsMapping = new FileSystemResource("src/test/resources/alien/variables/inputs_mapping_uber.yml");
        Map<String, Object> inputsMappingAsProperties = PropertiesYamlParser.ToMap.from(inputsMapping);
        Map<String, PropertyValue> inputsMappingFileResolved = inputsMappingFileVariableResolverConfigured.resolve(inputsMappingAsProperties, inputsPropertyDefinitions);

        assertThat(inputsMappingFileResolved).containsOnlyKeys("uber_input");
        assertThat(inputsMappingFileResolved.get("uber_input")).isInstanceOf(ComplexPropertyValue.class);


        assertThat(MapUtil.get(inputsMappingFileResolved.get("uber_input").getValue(), "complex")).isInstanceOf(Map.class);
        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.complex.complex_with_list.subfield2.sublist")).isInstanceOf(Collection.class);
        assertThat((Collection) MapUtil.get(inputsMappingFileResolved, "uber_input.complex.complex_with_list.subfield2.sublist")).hasSize(3);
        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.complex.complex_with_list.subfield2.sublist[0]")).isEqualTo("item 1");

        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.simple_var")).isInstanceOf(Map.class);
//        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.simple_var.int_field")).isEqualTo(1);
//        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.simple_var.float_field")).isEqualTo(3.14d);
        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.simple_var.string_field")).isEqualTo("text");
        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.simple_var.concat_field")).isEqualTo("13.14");

        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.simple_static")).isInstanceOf(Map.class);
        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.simple_static.int_field")).isEqualTo(51);
        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.simple_static.float_field")).isEqualTo(16.64d);
        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.simple_static.string_field")).isEqualTo("leffe");

//        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.complex.int_list")).isEqualTo(Arrays.asList(1L, 1L));
//        assertThat(MapUtil.get(inputsMappingFileResolved, "uber_input.complex.float_list")).isEqualTo(Arrays.asList(3.14d, 3.14d));
    }

/*
    uber_input:
        simple_var:
            int_field: ${int_variable}
            float_field: ${float_variable}
            string_field: ${string_variable}
            concat_field: ${int_variable}${float_variable}
        simple_static:
            int_field: 51
            float_field: 16.64
            string_field: "leffe"
        complex:
            complex_with_list: ${complex_with_list}
            int_list:
              - ${int_variable}
              - ${int_variable}
            float_list:
              - ${float_variable}
              - ${float_variable}
            mix_list:
              - ${int_variable}
              - ${float_variable}
              - ${string_variable}
            static_mix_list:
              - "jenlain"
              - 16.64
              - "kwak"
            complex_with_var_in_leaf: ${complex_with_var_in_leaf}
    */

}