package alien4cloud.variable;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import java.util.Properties;

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
 * It's possible to cast a Variable to a specific type here by using {@link #resolve(String, Class)}} and specifying the target class in that
 * case the conversion is done by Spring {@link org.springframework.core.convert.support.ConfigurableConversionService}
 * If the target class is Object.class no conversion will be made (that probably what you want).
 *
 * @see ObjectSourcesPropertyResolver
 * @see VariableSpelExpressionProcessor
 */
public class VariableResolver {

    private static final String APP_VARIABLES = "appVariables";
    private static final String ENV_VARIABLES = "envVariables";

    private PropertySourcesPropertyResolver resolver;
    private VariableSpelExpressionProcessor spelExpressionParser;
    private MutablePropertySources propertySources;

    public VariableResolver(PropertySource appVariables, PropertySource envVariables, PredefinedVariables predefinedVariables) {
        propertySources = new MutablePropertySources();
        // order matter
        propertySources.addLast(predefinedVariables);
        propertySources.addLast(envVariables);
        propertySources.addLast(appVariables);

        this.resolver = new ObjectSourcesPropertyResolver(propertySources);
        this.spelExpressionParser = new VariableSpelExpressionProcessor(this);
    }

    protected MutablePropertySources getPropertySources() {
        return propertySources;
    }

    public VariableResolver(Properties appVariables, Properties envVariables, PredefinedVariables predefinedVariables) {
        this(new PropertiesPropertySource(APP_VARIABLES, appVariables), new PropertiesPropertySource(ENV_VARIABLES, envVariables), predefinedVariables);
    }

    public <T> T resolve(String variableName, Class<T> clazz) {
        if (!resolver.containsProperty(variableName)) {
            throw new UnknownVariableException(variableName);
        }

        T resolved = resolver.getProperty(variableName, clazz);
        if (resolved != null && String.class.isAssignableFrom(resolved.getClass())) {
            // execute spel expression if any
            return spelExpressionParser.process((String) resolved, clazz);
        } else {
            return resolved;
        }
    }

    public String resolve(String variableName) {
        return resolve(variableName, String.class);
    }

}
