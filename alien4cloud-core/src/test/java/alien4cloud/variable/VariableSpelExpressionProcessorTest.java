package alien4cloud.variable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class VariableSpelExpressionProcessorTest {

    private VariableSpelExpressionProcessor variableSpelExpressionProcessor;

    @Before
    public void setUp() throws Exception {
        Properties appProps = new Properties();
        appProps.put("appVar", "appVar value");

        Map<String, Object> subEnvMap = Maps.newHashMap();
        subEnvMap.put("sub", "envVar value");
        Properties envProps = new Properties();
        envProps.put("envVar", subEnvMap);

        VariableResolver variableResolver = new VariableResolver(appProps, envProps, new PredefinedVariables());
        variableSpelExpressionProcessor = new VariableSpelExpressionProcessor(variableResolver);
    }

    @Test
    public void parse() throws Exception {
        assertThat(variableSpelExpressionProcessor.process("#{ #appVar }", String.class)).isEqualTo("appVar value");
        assertThat(variableSpelExpressionProcessor.process("#{ #envVar }", Object.class)).isEqualTo(ImmutableMap.of("sub", "envVar value"));
        assertThat(variableSpelExpressionProcessor.process("#{true == true}", Boolean.class)).isEqualTo(true);
        assertThat(variableSpelExpressionProcessor.process("#{true == false}", Boolean.class)).isEqualTo(false);
    }

}