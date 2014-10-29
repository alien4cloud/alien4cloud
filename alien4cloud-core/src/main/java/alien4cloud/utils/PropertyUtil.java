package alien4cloud.utils;

import java.util.Map;

import alien4cloud.tosca.container.model.template.PropertyValue;
import alien4cloud.tosca.model.PropertyDefinition;

import com.google.common.collect.Maps;

public final class PropertyUtil {
    private PropertyUtil() {
    }

    /**
     * Convert a map of property definitions to a map of property values based on the default values specified.
     * 
     * Note: This method will have to be removed once the ui manages properties correctly.
     * 
     * @param propertyDefinitions The map of {@link PropertyDefinition}s to convert.
     * @return An equivalent map of default {@link PropertyValue}s, that contains all properties definitions keys (default value is null when no default value
     *         is specified in the property definition).
     */
    public static Map<String, PropertyValue> getDefaultPropertyValuesFromPropertyDefinitions(Map<String, PropertyDefinition> propertyDefinitions) {
        if (propertyDefinitions == null) {
            return null;
        }

        Map<String, PropertyValue> defaultPropertyValues = Maps.newHashMap();

        for (Map.Entry<String, PropertyDefinition> entry : propertyDefinitions.entrySet()) {
            String defaultValue = entry.getValue().getDefault();
            if (defaultValue != null && !defaultValue.trim().isEmpty()) {
                defaultPropertyValues.put(entry.getKey(), new PropertyValue(defaultValue));
            } else {
                defaultPropertyValues.put(entry.getKey(), null);
            }
        }

        return defaultPropertyValues;
    }
}