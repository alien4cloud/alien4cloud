package alien4cloud.tosca.properties.constraints;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import alien4cloud.tosca.container.model.type.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Utility class to validate constraints types.
 */
public final class ConstraintUtil {

    private ConstraintUtil() {
    }

    /**
     * Validates that the {@link ToscaType} specified is a {@link ToscaType#STRING}.
     * 
     * @param propertyType The property tosca type.
     * @throws ConstraintValueDoNotMatchPropertyTypeException In case the type is not {@link ToscaType#STRING}.
     */
    public static void checkStringType(ToscaType propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        if (!ToscaType.STRING.equals(propertyType)) {
            throw new ConstraintValueDoNotMatchPropertyTypeException("Invalid property type <" + propertyType.toString() + ">");
        }
    }

    /**
     * Verify that the given tosca type is supported for comparison
     * 
     * @param propertyType the tosca type to check
     * @throws ConstraintValueDoNotMatchPropertyTypeException if the property type cannot be compared
     */
    public static void checkComparableType(ToscaType propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        // The validity of the value is already assured by us with our ToscaType.convert() method
        // here we just want to check that the constraint is not used on unsupported type as boolean
        switch (propertyType) {
        case FLOAT:
        case INTEGER:
        case TIMESTAMP:
        case VERSION:
            break;
        case STRING:
        case BOOLEAN:
            throw new ConstraintValueDoNotMatchPropertyTypeException("Constraint is invalid for property type <" + propertyType.toString() + ">");
        default:
            throw new ConstraintValueDoNotMatchPropertyTypeException("Invalid property type <" + propertyType.toString() + ">");
        }
    }

    /**
     * Convert a string value following its type throw exception if it cannot be converted to a comparable
     * 
     * @param propertyType the type of the property
     * @param value the value to convert
     * @return the converted comparable
     * @throws ConstraintValueDoNotMatchPropertyTypeException if the converted value is not a comparable
     */
    @SuppressWarnings("rawtypes")
    public static Comparable convertToComparable(ToscaType propertyType, String value) throws ConstraintValueDoNotMatchPropertyTypeException {
        Object comparableObj = propertyType.convert(value);
        if (!(comparableObj instanceof Comparable)) {
            throw new IllegalArgumentException("Try to convert a value of a type which is not comparable [" + propertyType + "] to Comparable");
        } else {
            return (Comparable) comparableObj;
        }
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @SuppressWarnings("PMD.UnusedPrivateField")
    public static class ConstraintInformation {
        private String name;
        private Object reference;
        private String value;
        private String type;
    }

    public static ConstraintInformation getConstraintInformation(Object constraint) throws IntrospectionException {
        PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(constraint.getClass()).getPropertyDescriptors();
        PropertyDescriptor firstDescriptor = null;
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            if (propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
                firstDescriptor = propertyDescriptor;
                break;
            }
        }
        if (firstDescriptor == null) {
            throw new IntrospectionException("Cannot find constraint name");
        }
        try {
            return new ConstraintInformation(firstDescriptor.getName(), firstDescriptor.getReadMethod().invoke(constraint), null, null);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IntrospectionException("Cannot retrieve constraint reference " + e.getMessage());
        }
    }
}