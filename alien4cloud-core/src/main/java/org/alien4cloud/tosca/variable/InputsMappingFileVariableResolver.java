package org.alien4cloud.tosca.variable;

import alien4cloud.tosca.context.ToscaContext;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link InputsMappingFileVariableResolver} is a specialized version of {@link VariableResolver}
 * that resolved inputs value and convert them into types matching {@link PropertyDefinition} attached to the inputs.
 * <p>
 * This class is thread safe because {@link #resolveAs(Map, Map, Converter)} is synchronized, anyway it better from
 * a performance perceptive to use multiple instances. Otherwise only 1 resolution can be done at the same time.
 */
@Slf4j
public class InputsMappingFileVariableResolver extends VariableResolver {

    private final static Pattern VARIABLE_NAME_IN_EXCEPTION_PATTERN = Pattern.compile("Could not resolve placeholder '(.*)' .*", Pattern.DOTALL);

    private ToscaTypeConverter converter = new ToscaTypeConverter(ToscaContext::get);

    public InputsMappingFileVariableResolver(PropertySource appVariables, PropertySource envVariables, PredefinedVariables predefinedVariables) {
        super(appVariables, envVariables, predefinedVariables);
    }

    public InputsMappingFileVariableResolver(Properties appVariables, Properties envVariables, PredefinedVariables predefinedVariables) {
        super(appVariables, envVariables, predefinedVariables);
    }

    public Map<String, Object> resolveAsValue(Map<String, Object> inputMappingMap, Map<String, PropertyDefinition> inputsDefinition) throws MissingVariablesException {
        return resolveAs(inputMappingMap, inputsDefinition, (resolvedPropertyValue, propertyDefinition) -> converter.toValue(resolvedPropertyValue, propertyDefinition));
    }

    public Map<String, PropertyValue> resolveAsPropertyValue(Map<String, Object> inputMappingMap, Map<String, PropertyDefinition> inputsDefinition) throws MissingVariablesException {
        return resolveAs(inputMappingMap, inputsDefinition, (resolvedPropertyValue, propertyDefinition) -> converter.toPropertyValue(resolvedPropertyValue, propertyDefinition));
    }

    // "synchronized" because we are altering the state of the parent VariableResolver that will alter the result of the whole class
    // "try/finally" has been added to ensure we go back to a proper state at this end
    private synchronized <T> Map<String, T> resolveAs(Map<String, Object> inputMappingMap, Map<String, PropertyDefinition> inputsDefinition, Converter<T> converter) throws MissingVariablesException {
        Map<String, T> resolved = Maps.newHashMap();
        MapPropertySource inputMappingMapPropertySource = new MapPropertySource("inputsMapping", inputMappingMap);
        getPropertySources().addFirst(inputMappingMapPropertySource);
        Set<String> missingVariables = Sets.newHashSet();
        Set<String> unresolvableInputs = Sets.newHashSet();

        try {
            for (String propertyName : inputMappingMapPropertySource.getPropertyNames()) {
                Object resolvedPropertyValue = null;
                try {
                    // resolved without converting to a specific type
                    resolvedPropertyValue = resolve(propertyName, Object.class);
                } catch (UnknownVariableException e) {
                    missingVariables.add(e.getVariableName());
                    unresolvableInputs.add(propertyName);
                    continue;
                } catch (IllegalArgumentException e) {
                    Matcher matcher = VARIABLE_NAME_IN_EXCEPTION_PATTERN.matcher(e.getMessage());
                    if (matcher.matches()) {
                        missingVariables.add(matcher.group(1));
                        unresolvableInputs.add(propertyName);
                    } else {
                        // unknown exception should be rethrow
                        throw new RuntimeException(e);
                    }
                    continue;
                }

                // convert result to the expected type according to inputsDefinition
                PropertyDefinition propertyDefinition = inputsDefinition.get(propertyName);
                T convertedPropertyValue;
                if (resolvedPropertyValue != null && propertyDefinition != null) {
                    convertedPropertyValue = converter.convert(resolvedPropertyValue, propertyDefinition);
                    if (convertedPropertyValue != null) {
                        resolved.put(propertyName, convertedPropertyValue);
                    }
                }
            }
        } finally {
            getPropertySources().remove(inputMappingMapPropertySource.getName());
        }

        if (missingVariables.size() > 0) {
            throw new MissingVariablesException(missingVariables, unresolvableInputs);
        }
        return resolved;
    }

    private interface Converter<T> {
        T convert(Object resolvedPropertyValue, PropertyDefinition propertyDefinition);
    }


}
