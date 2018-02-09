package org.alien4cloud.tosca.variable;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.apache.commons.collections4.MapUtils;
import org.springframework.core.env.MapPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.tosca.context.ToscaContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link InputsMappingFileVariableResolverInternal} is a specialized version of {@link VariableResolver}
 * that resolved inputs value and convert them into types respecting {@link PropertyDefinition} attached to the inputs.
 * <p>
 * Because {@link InputsMappingFileVariableResolverInternal} is not thread safe we want to enforce a new instance to be created each time we call a method.
 * This security net is enforced by the wrapping class {@link InputsMappingFileVariableResolver}
 */
@Slf4j
public class InputsMappingFileVariableResolver {

    private final static Pattern VARIABLE_NAME_IN_EXCEPTION_PATTERN = Pattern.compile("Could not resolve placeholder '(.*)' .*", Pattern.DOTALL);

    private final static ToscaTypeConverter DEFAULT_CONVERTER = new ToscaTypeConverter(ToscaContext::get);

    private InputsMappingFileVariableResolver() {
    }

    public static InputsMappingFileVariableResolverConfigured configure(Properties appVariables, Properties envTypeVariables, Properties envVariables, AlienContextVariables alienContextVariables) {
        return new InputsMappingFileVariableResolverConfigured(appVariables, envTypeVariables, envVariables, alienContextVariables);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class InputsMappingFileVariableResolverConfigured {
        private final Properties appVariables;
        private final Properties envTypeVariables;
        private final Properties envVariables;
        private final AlienContextVariables alienContextVariables;
        private ToscaTypeConverter customConverter;

        public InputsMappingFileVariableResolverConfigured customConverter(ToscaTypeConverter converter) {
            this.customConverter = converter;
            return this;
        }

        public InputsResolvingResult resolve(Map<String, Object> inputMappingMap, Map<String, PropertyDefinition> inputsDefinition) {
            if (MapUtils.isEmpty(inputsDefinition)) {
                return InputsResolvingResult.emptyInstance();
            }
            ToscaTypeConverter converter = customConverter != null ? customConverter : InputsMappingFileVariableResolver.DEFAULT_CONVERTER;
            return new InputsMappingFileVariableResolverInternal(converter, appVariables, envTypeVariables, envVariables, alienContextVariables).resolve(inputMappingMap, inputsDefinition);
        }
    }

    @AllArgsConstructor
    @Getter
    public static class InputsResolvingResult {
        private Map<String, PropertyValue> resolved;
        private Set<String> unresolved;
        private Set<String> missingVariables;

        public static InputsResolvingResult emptyInstance() {
            return new InputsResolvingResult(Maps.newHashMap(), Sets.newHashSet(), Sets.newHashSet());
        }
    }

    private static class InputsMappingFileVariableResolverInternal extends VariableResolver {

        private ToscaTypeConverter converter;

        private InputsMappingFileVariableResolverInternal(ToscaTypeConverter converter, Properties appVariables, Properties envTypeVariables, Properties envVariables, AlienContextVariables alienContextVariables) {
            super(appVariables, envTypeVariables, envVariables, alienContextVariables);
            this.converter = converter;
        }

        public InputsResolvingResult resolve(Map<String, Object> inputMappingMap, Map<String, PropertyDefinition> inputsDefinition) {
            Map<String, PropertyValue> resolved = Maps.newHashMap();
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
                    PropertyValue convertedPropertyValue;
                    if (resolvedPropertyValue != null && propertyDefinition != null) {
                        try {
                            convertedPropertyValue = converter.toPropertyValue(resolvedPropertyValue, propertyDefinition);
                        } catch (IllegalStateException e) {
                            // make the propertyValue based on the type of resolvedPropertyValue, and let the property constraint checker validate the type
                            // later
                            convertedPropertyValue = toDefaultType(resolvedPropertyValue);
                        }
                        if (convertedPropertyValue != null) {
                            resolved.put(propertyName, convertedPropertyValue);
                        }
                    }
                }
            } finally {
                getPropertySources().remove(inputMappingMapPropertySource.getName());
            }

            return new InputsResolvingResult(resolved, unresolvableInputs, missingVariables);
        }

        private PropertyValue toDefaultType(Object resolvedPropertyValue) {
            if (resolvedPropertyValue instanceof Map) {
                return new ComplexPropertyValue((Map) resolvedPropertyValue);
            } else if (resolvedPropertyValue instanceof Collection) {
                return new ListPropertyValue(Lists.newArrayList((Collection) resolvedPropertyValue));
            }
            return new ScalarPropertyValue(resolvedPropertyValue.toString());
        }
    }

}
