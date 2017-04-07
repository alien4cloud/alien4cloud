package alien4cloud.tosca.properties.constraints;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.normative.types.IComparablePropertyType;
import org.alien4cloud.tosca.normative.types.IPropertyType;
import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.normative.types.StringType;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;

/**
 * Utility class to validate constraints types.
 */
public final class ConstraintUtil {

    private ConstraintUtil() {
    }

    /**
     * Validates that the {@link IPropertyType} specified is a {@link StringType}.
     * 
     * @param propertyType The property tosca type.
     * @throws ConstraintValueDoNotMatchPropertyTypeException In case the type is not {@link StringType}.
     */
    public static void checkStringType(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        if (!StringType.NAME.equals(propertyType.getTypeName())) {
            throw new ConstraintValueDoNotMatchPropertyTypeException("Only string property type is accepted but found <" + propertyType.getTypeName() + ">");
        }
    }

    /**
     * Verify that the given tosca type is supported for comparison
     *
     * @param propertyType the tosca type to check
     * @throws ConstraintValueDoNotMatchPropertyTypeException if the property type cannot be compared
     */
    public static void checkComparableType(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        // The validity of the value is already assured by us with our ToscaType.convert() method
        // here we just want to check that the constraint is not used on unsupported type as boolean
        if (!(propertyType instanceof IComparablePropertyType)) {
            throw new ConstraintValueDoNotMatchPropertyTypeException(
                    "Constraint is invalid for property type <" + propertyType.getTypeName() + ">, as it's not comparable");
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
    public static Comparable convertToComparable(IPropertyType<?> propertyType, String value) throws ConstraintValueDoNotMatchPropertyTypeException {
        if (!(propertyType instanceof IComparablePropertyType)) {
            throw new ConstraintValueDoNotMatchPropertyTypeException(
                    "Constraint is invalid for property type <" + propertyType.getTypeName() + ">, as it's not comparable");
        }
        IComparablePropertyType<?> comparablePropertyType = (IComparablePropertyType) propertyType;
        try {
            return comparablePropertyType.parse(value);
        } catch (InvalidPropertyValueException e) {
            throw new ConstraintValueDoNotMatchPropertyTypeException("Unable to parse value <" + value + "> of type <" + propertyType.getTypeName() + ">", e);
        }
    }

    public static Object convert(IPropertyType<?> propertyType, String value) throws ConstraintValueDoNotMatchPropertyTypeException {
        try {
            return propertyType.parse(value);
        } catch (InvalidPropertyValueException e) {
            throw new ConstraintValueDoNotMatchPropertyTypeException("Unable to parse value <" + value + "> of type <" + propertyType.getTypeName() + ">", e);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConstraintInformation {
        private String name;
        private String path;
        private Object reference;
        private String value;
        private String type;

        public ConstraintInformation(String name, Object reference, String value, String type) {
            this.name = name;
            this.reference = reference;
            this.value = value;
            this.type = type;
        }

        @Override
        public String toString() {
            return "ConstraintInformation{" + "name='" + name + '\'' + ", path='" + path + '\'' + ", reference=" + reference + ", value='" + value + '\''
                    + ", type='" + type + '\'' + '}';
        }
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