package alien4cloud.utils;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.apache.commons.collections4.MapUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;

import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.rest.utils.JsonUtil;

public final class PropertyUtil {
    private PropertyUtil() {
    }

    /**
     * Convert a map of property definitions to a map of property values based on the default values specified.
     * <p/>
     * Note: This method will have to be removed once the ui manages properties correctly.
     *
     * @param propertyDefinitions The map of {@link PropertyDefinition}s to convert.
     * @return An equivalent map of default {@link ScalarPropertyValue}s, that contains all properties definitions keys (default
     *         value
     *         is null when no default value is specified in the property definition).
     */
    public static Map<String, AbstractPropertyValue> getDefaultPropertyValuesFromPropertyDefinitions(Map<String, PropertyDefinition> propertyDefinitions) {
        if (propertyDefinitions == null) {
            return null;
        }

        Map<String, AbstractPropertyValue> defaultPropertyValues = Maps.newLinkedHashMap();

        for (Map.Entry<String, PropertyDefinition> entry : propertyDefinitions.entrySet()) {
            defaultPropertyValues.put(entry.getKey(), getDefaultPropertyValueFromPropertyDefinition(entry.getValue()));
        }

        return defaultPropertyValues;
    }

    public static AbstractPropertyValue getDefaultPropertyValueFromPropertyDefinition(PropertyDefinition propertyDefinition) {
        if (propertyDefinition == null) {
            return null;
        }
        Object defaultValue = propertyDefinition.getDefault();
        if (defaultValue == null) {
            return null;
        }
        return (AbstractPropertyValue) defaultValue;
    }

    public static boolean setScalarDefaultValueIfNotNull(Map<String, String> properties, String key, AbstractPropertyValue abstractPropertyValue) {
        if (abstractPropertyValue != null && abstractPropertyValue instanceof ScalarPropertyValue) {
            properties.put(key, ((ScalarPropertyValue) abstractPropertyValue).getValue());
            return true;
        }
        return false;
    }

    public static void setScalarDefaultValueOrNull(Map<String, String> properties, String key, AbstractPropertyValue abstractPropertyValue) {
        if (abstractPropertyValue != null && abstractPropertyValue instanceof ScalarPropertyValue) {
            properties.put(key, ((ScalarPropertyValue) abstractPropertyValue).getValue());
        } else {
            properties.put(key, null);
        }
    }

    public static String getDefaultValueFromPropertyDefinitions(String propertyName, Map<String, PropertyDefinition> propertyDefinitions) {
        if (MapUtils.isNotEmpty(propertyDefinitions) && propertyDefinitions.containsKey(propertyName)) {
            return serializePropertyValue(propertyDefinitions.get(propertyName).getDefault());
        } else {
            return null;
        }
    }

    /**
     * Get the property from a complex path. If the path is simple, this method will return null.
     * A complex path is containing '.'
     *
     * @param propertyPath the complex property path
     * @return the first element of the path (property name)
     */
    public static String getPropertyNameFromComplexPath(String propertyPath) {
        if (propertyPath.contains(".")) {
            String[] paths = propertyPath.split("\\.");
            return paths[0];
        } else {
            return null;
        }
    }

    /**
     * Get the scalar value
     *
     * @param propertyValue the property value
     * @throws alien4cloud.paas.exception.NotSupportedException if called on a non ScalarPropertyValue
     * @return the value or null if the propertyValue is null
     */
    public static String getScalarValue(AbstractPropertyValue propertyValue) {
        if (propertyValue == null) {
            return null;
        } else if (propertyValue instanceof ScalarPropertyValue) {
            return ((ScalarPropertyValue) propertyValue).getValue();
        } else {
            throw new NotSupportedException("Property value is not of type scalar");
        }
    }

    public static String serializePropertyValue(Object value) {
        try {
            if (value == null) {
                return null;
            } else if (value instanceof String) {
                return (String) value;
            } else if (value instanceof PropertyValue) {
                PropertyValue pv = (PropertyValue) value;
                if (pv instanceof ScalarPropertyValue) {
                    return ((ScalarPropertyValue) pv).getValue();
                } else {
                    return pv.getValue() == null ? null : JsonUtil.toString(pv.getValue());
                }
            } else {
                return JsonUtil.toString(value);
            }
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}