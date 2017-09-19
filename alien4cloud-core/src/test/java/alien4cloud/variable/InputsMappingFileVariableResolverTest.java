package alien4cloud.variable;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import alien4cloud.model.application.Application;

import static org.assertj.core.api.Assertions.*;

public class InputsMappingFileVariableResolverTest {

    private InputsMappingFileVariableResolver inputsMappingFileVariableResolver;

    private Map<String, PropertyDefinition> inputsPropertyDefinitions;

    @Before
    public void setUp() throws Exception {
        inputsPropertyDefinitions = Maps.newHashMap();
        inputsPropertyDefinitions.put("int_input", propertyDefinitionWithType(ToscaTypes.INTEGER));
        inputsPropertyDefinitions.put("float_input", propertyDefinitionWithType(ToscaTypes.FLOAT));
        inputsPropertyDefinitions.put("string_input", propertyDefinitionWithType(ToscaTypes.STRING));
        inputsPropertyDefinitions.put("complex_input", propertyDefinitionWithType(ToscaTypes.MAP));

        Resource yamlApp = new FileSystemResource("src/test/resources/alien/variables/variables_app_test.yml");
        Resource yamlEnv = new FileSystemResource("src/test/resources/alien/variables/variables_env_test.yml");

        PredefinedVariables predefinedVariables = new PredefinedVariables();
        Application application = new Application();
        application.setName("originalAppName");
        predefinedVariables.setApplication(application);

        inputsMappingFileVariableResolver = new InputsMappingFileVariableResolver(YamlParser.ToProperties.from(yamlApp), YamlParser.ToProperties.from(yamlEnv),
                predefinedVariables);
    }

    private PropertyDefinition propertyDefinitionWithType(String type){
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType(type);
        return propertyDefinition;
    }

    @Test
    public void check_inputs_mapping_can_be_parsed_when_no_variable() throws Exception {
        Resource inputsMapping = new FileSystemResource("src/test/resources/alien/variables/inputs_mapping_without_variable.yml");
        Map<String, Object> inputsMappingAsProperties = YamlParser.ToMap.from(inputsMapping);
        Map<String, Object> inputsMappingFileResolved = inputsMappingFileVariableResolver.resolveInputsMappingFile(inputsMappingAsProperties, inputsPropertyDefinitions);

        assertThat(inputsMappingFileResolved).containsOnlyKeys(Iterables.toArray(inputsMappingAsProperties.keySet(), String.class));
        assertThat(inputsMappingFileResolved.get("int_input")).isEqualTo(10);
        assertThat(inputsMappingFileResolved.get("float_input")).isEqualTo(3.14);
        assertThat(inputsMappingFileResolved.get("string_input")).isEqualTo("text");
        assertThat(inputsMappingFileResolved.get("complex_input")).isEqualTo(
                ImmutableMap.of(
                        "sub1", ImmutableMap.of("subfield11", 11, "subfield12", 12),
                        "sub2", ImmutableMap.of("subfield21", 21),
                        "field01", "01")
                );
    }

    @Test
    public void check_inputs_mapping_can_be_parsed_when_variable() throws Exception {
        Resource inputsMapping = new FileSystemResource("src/test/resources/alien/variables/inputs_mapping_with_variables.yml");
        Map<String, Object> inputsMappingAsProperties = YamlParser.ToMap.from(inputsMapping);
        Map<String, Object> inputsMappingFileResolved = inputsMappingFileVariableResolver.resolveInputsMappingFile(inputsMappingAsProperties, inputsPropertyDefinitions);

        assertThat(inputsMappingFileResolved).containsOnlyKeys(Iterables.toArray(inputsMappingAsProperties.keySet(), String.class));
        assertThat(inputsMappingFileResolved.get("int_input")).isEqualTo(1L); // yeah int returns a long
        assertThat(inputsMappingFileResolved.get("float_input")).isEqualTo(3.14);
        assertThat(inputsMappingFileResolved.get("string_input")).isEqualTo("text_3.14");
        assertThat(inputsMappingFileResolved.get("complex_input")).isEqualTo(
                ImmutableMap.of(
                        "sub1", ImmutableMap.of("complex", ImmutableMap.of("subfield" , "text")),
                        "sub2", ImmutableMap.of("subfield21", "1"),
                        "field01", "text")
        );
    }

    @Test
    public void check_uber_input_can_be_parsed() throws Exception {
        Resource inputsMapping = new FileSystemResource("src/test/resources/alien/variables/inputs_mapping_uber.yml");
        Map<String, Object> inputsMappingAsProperties = YamlParser.ToMap.from(inputsMapping);
        Map<String, Object> inputsMappingFileResolved = inputsMappingFileVariableResolver.resolveInputsMappingFile(inputsMappingAsProperties, inputsPropertyDefinitions);

        assertThat(inputsMappingFileResolved).containsOnlyKeys("uber_input");
        /*
              "simple_var" ,"simple_var.int_field", "simple_var.float_field", "simple_var.string_field", "simple_var.concat_field",
                "simple_static", "simple_static.int_field", "simple_static.float_field", "simple_static.string_field",
                "complex", "complex.complex_with_list", "complex.int_list", "complex.float_list", "complex.mix_list", "complex.static_mix_list",  "complex.complex_with_var_in_leaf"
        );*/

        assertThat(inputsMappingFileResolved.get("int_input")).isEqualTo(1L); // yeah int returns a long
        assertThat(inputsMappingFileResolved.get("float_input")).isEqualTo(3.14);
        assertThat(inputsMappingFileResolved.get("string_input")).isEqualTo("text_3.14");
        assertThat(inputsMappingFileResolved.get("complex_input")).isEqualTo(
                ImmutableMap.of(
                        "sub1", ImmutableMap.of("complex", ImmutableMap.of("subfield" , "text")),
                        "sub2", ImmutableMap.of("subfield21", "1"),
                        "field01", "text")
        );
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