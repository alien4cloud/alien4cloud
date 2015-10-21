package alien4cloud.utils.services;

import java.util.List;
import java.util.Map;

import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.normative.*;

@Slf4j
public class PropertyValueService {

    /**
     * Extract the value from a sub-path of a property.
     * 
     * @param propertyValue
     *            The value of the property.
     * @param path
     *            The path to get the value.
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
     * @return
     */
    public static String getValueInUnit(Object propertyValue, String unit, PropertyDefinition propertyDefinition) {
        // TODO manage complex objects and sub-paths
        // @param path The optional sub-path of the property (may be null or empty).
        if (propertyValue instanceof String) {
            return getValueInUnit((String) propertyValue, unit, propertyDefinition.getType());
        } else if (propertyValue instanceof List) {
            log.error("Convertion of unit is currently not supported for complex properties");
            throw new NotImplementedException();
        } else if (propertyValue instanceof Map) {
            log.error("Convertion of unit is currently not supported for complex properties");
            throw new NotImplementedException();
        } else {
            throw new InvalidArgumentException(
                    "Not expecting to receive unit convertion for other types than String, Map or List as " + propertyValue.getClass().getName());
        }
    }

    private static String getValueInUnit(String propertyValue, String unit, String toscaType) {
        IPropertyType type = ToscaType.fromYamlTypeName(toscaType);
        if (type instanceof ScalarType) {
            try {
                ScalarUnit scalarUnit = ((ScalarType) type).parse(propertyValue);
                return String.valueOf(scalarUnit.convert(unit));
            } catch (InvalidPropertyValueException e) {
                log.error("e");
                throw new InvalidArgumentException(e.getMessage());
            }
        }
        throw new InvalidArgumentException("Type is not a scalar type");
    }
}