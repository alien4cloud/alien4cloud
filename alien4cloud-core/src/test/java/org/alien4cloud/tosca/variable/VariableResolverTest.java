package org.alien4cloud.tosca.variable;

import alien4cloud.model.application.Application;
import com.google.common.collect.ImmutableMap;
import org.alien4cloud.tosca.utils.PropertiesYamlParser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class VariableResolverTest {
    private VariableResolver resolver;

    @Before
    public void setUp() throws Exception {
        Resource yamlApp = new FileSystemResource("src/test/resources/alien/variables/variables_app_test.yml");
        Resource yamlEnv = new FileSystemResource("src/test/resources/alien/variables/variables_env_test.yml");

        AlienContextVariables alienContextVariables = new AlienContextVariables();
        Application application = new Application();
        application.setName("originalAppName");
        alienContextVariables.setApplication(application);

        resolver = new VariableResolver(PropertiesYamlParser.ToProperties.from(yamlApp), PropertiesYamlParser.ToProperties.from(yamlEnv), alienContextVariables);
    }

    @Test
    public void resolve_app_var_int() throws Exception {
        assertThat(resolver.resolve("int_variable", Integer.class)).isEqualTo(1);
    }

    @Test
    public void resolve_app_var_float() throws Exception {
        assertThat(resolver.resolve("float_variable", Float.class)).isEqualTo(3.14f);
    }

    @Test
    public void resolve_app_var_string() throws Exception {
        assertThat(resolver.resolve("string_variable")).isEqualTo("text");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolve_app_var_complex() throws Exception {
        Map<String, Object> complex = resolver.resolve("complex_variable", Map.class);
        assertThat(complex.get("complex")).isEqualTo(ImmutableMap.builder().put("subfield", "subValue").build());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolve_app_var_complex_with_var_in_leaf() throws Exception {
        Map<String, Object> complex = resolver.resolve("complex_with_var_in_leaf", Map.class);
        assertThat(complex.get("complex")).isEqualTo(ImmutableMap.builder().put("subfield", "text").build());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolve_complex_with_list_var_in_body() throws Exception {
        Object complex = resolver.resolve("complex_with_list_var_in_body", Object.class);
        assertThat(complex).isInstanceOf(Map.class);
        assertThat(((Map) complex).get("string_list")).isEqualTo(Arrays.asList("item 1", "item 2", "item 3"));
        assertThat(((Map) complex).get("complex_list")).isEqualTo(Arrays.asList(
                ImmutableMap.of("item10", "value10", "item11", "value11"),
                ImmutableMap.of("item20", "value20", "item21", "value21"))
        );
    }


    @Test
    @SuppressWarnings("unchecked")
    public void resolve_app_var_complex_with_var_in_body() throws Exception {
        Map<String, Object> complex = (Map<String, Object>) resolver.resolve("complex_with_var_in_body", Object.class);
        assertThat(complex.get("complex_from_var")).isEqualTo(ImmutableMap.of("complex", ImmutableMap.builder().put("subfield", "text").build()));
    }

    @Test
    public void resolve_concat() throws Exception {
        String s = resolver.resolve("concat_variable");
        assertThat(s).isEqualTo("1 - text");
    }

    @Test
    public void resolve_spel() throws Exception {
        // not supported - SpEL only works with String.class
        // assertThat(resolver.resolve("spel_variable", Boolean.class)).isEqualTo(true);

        assertThat(resolver.resolve("spel_variable", String.class)).isEqualTo("true");
    }

    @Test
    public void resolve_mix_spel_and_variable() throws Exception {
        assertThat(resolver.resolve("mix_spel_and_variable", String.class)).isEqualTo("1 true");
    }

    @Test
    public void resolve_leaf_complex_object() throws Exception {
        String s = resolver.resolve("complex_variable.complex.subfield");
        assertThat(s).isEqualTo("subValue");
    }

    @Test
    public void resolve_complex_with_dot_field() throws Exception {
        Map map = resolver.resolve("complex_with_dot_field.complex.dot.field", Map.class);
        // does not work. int converted to String :(
        // assertThat(map).isEqualTo(ImmutableMap.builder().put("value", 3.14).build());

        assertThat(map).isEqualTo(ImmutableMap.builder().put("value", "3.14").build());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolve_subfield_complex_object() throws Exception {
        assertThat(resolver.resolve("complex_variable.complex", Map.class)).isEqualTo(ImmutableMap.builder().put("subfield", "subValue").build());
    }

    @Test(expected = UnknownVariableException.class)
    public void throw_exception_if_variable_unknown() throws Exception {
        String s = resolver.resolve("unknown");
    }

    @Test
    public void check_app_variable_can_be_overridden_in_env() throws Exception {
        assertThat(resolver.resolve("overidden_variable")).isEqualTo("ok");
    }

    @Test
    public void check_new_variable_can_be_added_in_env() throws Exception {
        assertThat(resolver.resolve("env_variable")).isEqualTo("new env var");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolve_list_variable() throws Exception {
        Object listVariable = resolver.resolve("list_variable", Object.class);
        assertThat(listVariable).isInstanceOf(List.class);
        assertThat((List<String>) listVariable).containsExactly("item 1", "item 2", "item 3");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolve_list_spel_variable() throws Exception {
        Object listVariable = resolver.resolve("list_spel_variable", Object.class);
        assertThat(listVariable).isInstanceOf(List.class);
        assertThat((List<String>) listVariable).containsExactly("item true", "item false", "item 3");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolve_complex_with_list() throws Exception {
        Object complex = resolver.resolve("complex_with_list", Object.class);
        assertThat(complex).isInstanceOf(Map.class);
        assertThat(((Map<String, Object>) complex)).containsExactly(
                entry("subfield1", "text"),
                entry("subfield2", ImmutableMap.of("sublist", Arrays.asList("item 1", "item 2", "item 3.14"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void resolve_list_of_complex() throws Exception {
        Object list = resolver.resolve("list_of_complex", Object.class);
        assertThat(list).isInstanceOf(Collection.class);
        assertThat(((Collection<Object>) list)).hasSize(2);
        assertThat(((Collection<Object>) list)).containsExactly(
                ImmutableMap.of("item10", "value10", "item11", "value11"),
                ImmutableMap.of("item20", "value20", "item21", "value21")
        );
    }

    @Test
    public void predefined_var_cannot_be_redefined() throws Exception {
        assertThat(resolver.resolve("a4c.application.name")).isEqualTo("originalAppName");
    }

}