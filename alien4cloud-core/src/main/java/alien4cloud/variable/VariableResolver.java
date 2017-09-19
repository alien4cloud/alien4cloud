package alien4cloud.variable;

import java.util.Properties;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

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
