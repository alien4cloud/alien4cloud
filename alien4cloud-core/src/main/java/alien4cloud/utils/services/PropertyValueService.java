package alien4cloud.utils.services;

import alien4cloud.exception.InvalidArgumentException;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.ScalarType;
import alien4cloud.tosca.normative.ScalarUnit;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.utils.MapUtil;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyValueService {

    /**
     * Extract the value from a sub-path of a property.
     * 
     * @param propertyValue The value of the property.
     * @param path The path to get the value.
     * @return The value at the given path or propertyValue if the path is null or empty.
     */
    public static Object getValue(Object propertyValue, String path) {
        if (path == null || path.isEmpty()) {
            return propertyValue;
        }
        return MapUtil.get(propertyValue, path);
    }

    /**
     * Get a property value in the given unit.
     *
     * @param propertyValue The property value from which to get the value in a specified unit
     * @param unit The unit in which to get the value.
     * @param propertyDefinition The property definition of the root property.
     * @return The value in the correct unit.
     */
    public static String getValueInUnit(Object propertyValue, String unit, boolean ceil, PropertyDefinition propertyDefinition) {
        // TODO manage complex objects and sub-paths
        // @param path The optional sub-path of the property (may be null or empty).
        if (propertyValue instanceof String) {
            return getValueInUnit((String) propertyValue, unit, ceil, propertyDefinition.getType());
        } else if (propertyValue instanceof List) {
            log.error("Conversion of unit is currently not supported for complex properties");
            throw new NotSupportedException("Conversion of unit is currently not supported for complex properties");
        } else if (propertyValue instanceof Map) {
            log.error("Conversion of unit is currently not supported for complex properties");
            throw new NotSupportedException("Conversion of unit is currently not supported for complex properties");
        } else {
            throw new InvalidArgumentException(
                    "Not expecting to receive unit conversion for other types than String, Map or List as " + propertyValue.getClass().getName());
        }
    }

    private static String getValueInUnit(String propertyValue, String unit, boolean ceil, String toscaType) {
        IPropertyType type = ToscaType.fromYamlTypeName(toscaType);
        if (type instanceof ScalarType) {
            try {
                ScalarUnit scalarUnit = ((ScalarType) type).parse(propertyValue);
                double convertedValue = scalarUnit.convert(unit);
                if (ceil) {
                    convertedValue = Math.ceil(convertedValue);
                }
                return format(convertedValue);
            } catch (InvalidPropertyValueException e) {
                log.error("e");
                throw new InvalidArgumentException(e.getMessage());
            }
        }
        throw new InvalidArgumentException("Type is not a scalar type");
    }

    private static String format(double d) {
        if (d == (long) d)
            return String.format("%d", (long) d);
        else
            return String.format("%s", d);
    }
}