package org.alien4cloud.tosca.variable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class SpelExpressionProcessorTest {

    private SpelExpressionProcessor spelExpressionProcessor;

    @Before
    public void setUp() throws Exception {
        Map<String, Object> subEnvMap = Maps.newHashMap();
        subEnvMap.put("sub", "subvar value");
        Properties envProps = new Properties();
        envProps.put("var2", subEnvMap);
        envProps.put("var1", "var1 value");

        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addLast(new PropertiesPropertySource("testVar", envProps));

        spelExpressionProcessor = new SpelExpressionProcessor(new PropertySourcesPropertyResolver(propertySources));
    }

    @Test
    public void parse() throws Exception {
        assertThat(spelExpressionProcessor.process("#{ #var1 }", String.class)).isEqualTo("var1 value");
        assertThat(spelExpressionProcessor.process("#{ #var2 }", Object.class)).isEqualTo(ImmutableMap.of("sub", "subvar value"));
        assertThat(spelExpressionProcessor.process("#{true == true}", Boolean.class)).isEqualTo(true);
        assertThat(spelExpressionProcessor.process("#{true == false}", Boolean.class)).isEqualTo(false);
    }

}