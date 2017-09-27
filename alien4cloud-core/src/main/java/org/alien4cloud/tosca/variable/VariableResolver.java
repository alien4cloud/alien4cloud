package org.alien4cloud.tosca.variable;

import java.util.Properties;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

/**
 * A Variable as a 'name' and a 'value".
 * <p>
 * A Variable name is a {@link String}
 * <p>
 * A Variable value is a {@link Object} and can be made from:
 * - static primitive value (i.e: 10, "my string")
 * - another variable
 * - a SpEL expression
 * - a concatenation of anything above and the result of the concatenation will be a {@link String}
 * - static complex value (i.e: list of item, map of item). item can also be build using Variables
 * <p>
 * A Variable can be overridden by a new definition in a lower level.
 * <p>
 * Resolving a Variable means building the effective value of the Variable (after replacing inner Variables with there
 * effective values, executing SpEL expression).
 * <p>
 * It's possible to cast a Variable to a specific type here by using {@link #resolve(String, Class)}} and specifying
 * the target class in that case the conversion is done by Spring {@link org.springframework.core.convert.support.ConfigurableConversionService}.
 * If the target class is Object.class no conversion will be made (that probably what you want an do the conversion later
 * to match a {@link org.alien4cloud.tosca.model.definitions.PropertyDefinition}).
 *
 * @see ObjectSourcesPropertyResolver
 * @see SpelExpressionProcessor
 */
public class VariableResolver {

    private static final String APP_VARIABLES = "appVariables";
    private static final String ENV_VARIABLES = "envVariables";
    private static final String ENV_TYPE_VARIABLES = "envTypeVariables";

    private PropertySourcesPropertyResolver resolver;
    private MutablePropertySources propertySources;

    public VariableResolver(PropertySource appVariables, PropertySource envTypeVariables, PropertySource envVariables, AlienContextVariables alienContextVariables) {
        propertySources = new MutablePropertySources();
        // order matter
        propertySources.addLast(alienContextVariables);
        propertySources.addLast(envVariables);
        propertySources.addLast(envTypeVariables);
        propertySources.addLast(appVariables);

        this.resolver = new ObjectSourcesPropertyResolver(propertySources);
    }

    protected MutablePropertySources getPropertySources() {
        return propertySources;
    }

    public VariableResolver(Properties appVariables, Properties envTypeVariables, Properties envVariables, AlienContextVariables alienContextVariables) {
        this(new PropertiesPropertySource(APP_VARIABLES, appVariables), new PropertiesPropertySource(ENV_TYPE_VARIABLES, envTypeVariables), new PropertiesPropertySource(ENV_VARIABLES, envVariables), alienContextVariables);
    }

    public <T> T resolve(String variableName, Class<T> clazz) {
        if (!resolver.containsProperty(variableName)) {
            throw new UnknownVariableException(variableName);
        }

        return resolver.getProperty(variableName, clazz);
    }

    public String resolve(String variableName) {
        return resolve(variableName, String.class);
    }

}
