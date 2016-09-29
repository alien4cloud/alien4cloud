package alien4cloud.utils;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MapUtils;

import com.google.common.collect.Maps;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;

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

    /**
     * TODO: should be removed !
     */
    @Deprecated
    public static String getDefaultValueFromPropertyDefinitions(String propertyName, Map<String, PropertyDefinition> propertyDefinitions) {
        if (MapUtils.isNotEmpty(propertyDefinitions) && propertyDefinitions.containsKey(propertyName)) {
            return propertyDefinitions.get(propertyName).getDefault().toString();
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
     * Merge from map into 'into' map
     *
     * @param from from map
     * @param into into map
     * @param keysToConsider if defined only keys contained by this set are considered
     */
    public static void mergeProperties(Map<String, AbstractPropertyValue> from, Map<String, AbstractPropertyValue> into, Set<String> keysToConsider) {
        if (MapUtils.isNotEmpty(from)) {
            for (Map.Entry<String, AbstractPropertyValue> fromEntry : from.entrySet()) {
                if (keysToConsider != null && !keysToConsider.contains(fromEntry.getKey())) {
                    // If the key filter do not contain the key then do not consider
                    continue;
                }
                AbstractPropertyValue existingValue = into.get(fromEntry.getKey());
                if (fromEntry.getValue() != null || existingValue == null) {
                    into.put(fromEntry.getKey(), fromEntry.getValue());
                }
            }
        }
    }
}