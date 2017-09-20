package alien4cloud.variable;

import alien4cloud.tosca.context.ToscaContext;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.normative.types.IPropertyType;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Map;
import java.util.Properties;

@Slf4j
public class InputsMappingFileVariableResolver extends VariableResolver {

    private ToscaTypeConverter converter = new ToscaTypeConverter(ToscaContext::get);

    public InputsMappingFileVariableResolver(PropertySource appVariables, PropertySource envVariables, PredefinedVariables predefinedVariables) {
        super(appVariables, envVariables, predefinedVariables);
    }

    public InputsMappingFileVariableResolver(Properties appVariables, Properties envVariables, PredefinedVariables predefinedVariables) {
        super(appVariables, envVariables, predefinedVariables);
    }

    public synchronized Map<String, Object> resolveInputsMappingFile(Map<String, Object> inputMappingMap, Map<String, PropertyDefinition> inputsDefinition) {
        Map<String, Object> resolved = Maps.newHashMap();
        MapPropertySource inputMappingMapPropertySource = new MapPropertySource("inputsMapping", inputMappingMap);
        getPropertySources().addFirst(inputMappingMapPropertySource);
        for (String propertyName : inputMappingMapPropertySource.getPropertyNames()) {
            // resolved without converting to a specific type
            Object resolvedPropertyValue = resolve(propertyName, Object.class);

            // convert result to the expected type according to inputsDefinition
            PropertyDefinition propertyDefinition = inputsDefinition.get(propertyName);
            Object value;
            if (resolvedPropertyValue != null && propertyDefinition != null) {
                value = converter.toValue(resolvedPropertyValue, propertyDefinition);
                if (value != null) {
                    resolved.put(propertyName, value);
                }
            }
        }
        getPropertySources().remove(inputMappingMapPropertySource.getName());

        return resolved;
    }

    public synchronized Map<String, PropertyValue> resolveInputsMappingFilePropertyValue(Map<String, Object> inputMappingMap,
            Map<String, PropertyDefinition> inputsDefinition) {
        Map<String, PropertyValue> resolved = Maps.newHashMap();
        MapPropertySource inputMappingMapPropertySource = new MapPropertySource("inputsMapping", inputMappingMap);
        getPropertySources().addFirst(inputMappingMapPropertySource);
        for (String propertyName : inputMappingMapPropertySource.getPropertyNames()) {
            // resolved without converting to a specific type
            Object resolvedPropertyValue = resolve(propertyName, Object.class);

            // toPropertyValue result to the expected type according to inputsDefinition
            PropertyDefinition propertyDefinition = inputsDefinition.get(propertyName);
            PropertyValue convertedPropertyValue;
            if (resolvedPropertyValue != null && propertyDefinition != null) {
                convertedPropertyValue = converter.toPropertyValue(resolvedPropertyValue, propertyDefinition);
                if (convertedPropertyValue != null) {
                    resolved.put(propertyName, convertedPropertyValue);
                }
            }
        }
        getPropertySources().remove(inputMappingMapPropertySource.getName());

        return resolved;
    }

    private void convertToExpectedType(Map<String, PropertyDefinition> inputsDefinition, Map<String, Object> resolved, String propertyName,
            Object resolvedPropertyValue) {
        PropertyDefinition propertyDefinition = inputsDefinition.get(propertyName);
        if (resolvedPropertyValue != null && propertyDefinition != null) {
            IPropertyType<?> toscaType = ToscaTypes.fromYamlTypeName(propertyDefinition.getType());
            try {
                resolvedPropertyValue = toscaType.parse(resolvedPropertyValue.toString());
                resolved.put(propertyName, resolvedPropertyValue);
            } catch (InvalidPropertyValueException e) {
                log.warn("Failed to parse input named <" + propertyName + "> with value <" + resolvedPropertyValue.toString() + "> as <"
                        + propertyDefinition.getType() + ">");
            }
        }
    }
}
