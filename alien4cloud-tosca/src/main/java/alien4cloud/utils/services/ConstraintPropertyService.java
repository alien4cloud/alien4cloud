package alien4cloud.utils.services;

import static alien4cloud.utils.AlienUtils.safe;

import java.beans.IntrospectionException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import alien4cloud.exception.InvalidArgumentException;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.PrimitiveDataType;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.tosca.properties.constraints.exception.ConstraintTechnicalException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.InvalidVersionException;
import lombok.extern.slf4j.Slf4j;

/**
 * Common property constraint utils
 */
@Slf4j
@Service
public class ConstraintPropertyService {

    /**
     * Check the constraints on an unwrapped property value (basically a string, map or list).
     * 
     * @param propertyName The name of the property.
     * @param propertyValue The value of the property to check.
     * @param propertyDefinition The property definition that defines the property to check.
     * @throws ConstraintValueDoNotMatchPropertyTypeException In case the value type doesn't match the type of the property as defined.
     * @throws ConstraintViolationException In case the value doesn't match one of the constraints defined on the property.
     */
    public void checkPropertyConstraint(String propertyName, Object propertyValue, PropertyDefinition propertyDefinition)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        checkPropertyConstraint(propertyName, propertyValue, propertyDefinition, null);
    }

    /**
     * Check the constraints on an unwrapped property value (basically a string, map or list) and get events through the given consumer parameter when missing
     * properties on complex data type are found.
     * Note that the property value cannot be null and the required characteristic of the initial property definition will NOT be checked.
     *
     * @param propertyName The name of the property.
     * @param propertyValue The value of the property to check.
     * @param propertyDefinition The property definition that defines the property to check.
     * @param missingPropertyConsumer A consumer to receive events when a required property is not defined on a complex type sub-field.
     * @throws ConstraintValueDoNotMatchPropertyTypeException In case the value type doesn't match the type of the property as defined.
     * @throws ConstraintViolationException In case the value doesn't match one of the constraints defined on the property.
     */
    public void checkPropertyConstraint(String propertyName, Object propertyValue, PropertyDefinition propertyDefinition,
            Consumer<String> missingPropertyConsumer) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        boolean isPrimitiveType = false;
        boolean isTypeDerivedFromPrimitive = false;
        DataType dataType = null;
        String typeName = propertyDefinition.getType();
        if (ToscaType.isPrimitive(typeName)) {
            isPrimitiveType = true;
        } else {
            dataType = ToscaContext.get(DataType.class, typeName);
            if (dataType instanceof PrimitiveDataType) {
                // the type is derived from a primitive type
                isTypeDerivedFromPrimitive = true;
            }
        }
        if (propertyValue instanceof String) {
            if (isPrimitiveType) {
                checkSimplePropertyConstraint(propertyName, (String) propertyValue, propertyDefinition);
            } else if (isTypeDerivedFromPrimitive) {
                checkComplexPropertyDerivedFromPrimitiveTypeConstraints(propertyName, (String) propertyValue, propertyDefinition, dataType);
            } else {
                throw new ConstraintValueDoNotMatchPropertyTypeException(
                        "Property value is a String while the expected data type is a complex type " + propertyValue.getClass().getName());
            }
        } else if (propertyValue instanceof Map) {
            checkComplexPropertyConstraint(propertyName, (Map<String, Object>) propertyValue, propertyDefinition, missingPropertyConsumer);
        } else if (propertyValue instanceof List) {
            checkListPropertyConstraint(propertyName, (List<Object>) propertyValue, propertyDefinition, missingPropertyConsumer);
        } else {
            throw new InvalidArgumentException(
                    "Not expecting to receive constraint validation for other types than String, Map or List as " + propertyValue.getClass().getName());
        }
    }

    /**
     * Check constraints defined on a property for a specified value
     *
     * @param propertyName Property name (mainly used to create a comprehensive error message)
     * @param stringValue Tested property value
     * @param propertyDefinition Full property definition with type, constraints, default value,...
     * @throws ConstraintViolationException
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     */
    public void checkSimplePropertyConstraint(final String propertyName, final String stringValue, final PropertyDefinition propertyDefinition)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        ConstraintInformation consInformation = null;

        // check any property definition without constraints (type/value)
        checkBasicType(propertyName, propertyDefinition.getType(), stringValue);

        if (propertyDefinition.getConstraints() != null && !propertyDefinition.getConstraints().isEmpty()) {
            IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(propertyDefinition.getType());
            for (PropertyConstraint constraint : propertyDefinition.getConstraints()) {
                try {
                    consInformation = ConstraintUtil.getConstraintInformation(constraint);
                    consInformation.setPath(propertyName + ".constraints[" + consInformation.getName() + "]");
                    constraint.initialize(toscaType);
                    constraint.validate(toscaType, stringValue);
                } catch (ConstraintViolationException e) {
                    throw new ConstraintViolationException(e.getMessage(), e, consInformation);
                } catch (IntrospectionException e) {
                    // ConstraintValueDoNotMatchPropertyTypeException is not supposed to be raised here (only in constraint definition validation)
                    log.info("Constraint introspection error for property <" + propertyName + "> value <" + stringValue + ">", e);
                    throw new ConstraintTechnicalException("Constraint introspection error for property <" + propertyName + "> value <" + stringValue + ">", e);
                }
            }
        }
    }

    /**
     * Check constraints defined on a property which has a type derived from a primitive.
     */
    private void checkComplexPropertyDerivedFromPrimitiveTypeConstraints(final String propertyName, final String stringValue,
            final PropertyDefinition propertyDefinition, final DataType dataType)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        ConstraintInformation consInformation = null;
        boolean hasDefinitionConstraints = propertyDefinition.getConstraints() != null && !propertyDefinition.getConstraints().isEmpty();
        boolean hasTypeConstraints = false;
        if (dataType instanceof PrimitiveDataType && ((PrimitiveDataType) dataType).getConstraints() != null
                && !((PrimitiveDataType) dataType).getConstraints().isEmpty()) {
            hasTypeConstraints = true;
        }
        String derivedFromPrimitiveType = dataType.getDerivedFrom().get(0);
        // Check the type of the property even if there is no constraints.
        checkBasicType(propertyName, derivedFromPrimitiveType, stringValue);
        if (hasDefinitionConstraints || hasTypeConstraints) { // check the constraints if there is any defined
            if (hasDefinitionConstraints) {
                checkConstraints(propertyName, stringValue, derivedFromPrimitiveType, propertyDefinition.getConstraints());
            }
            if (hasTypeConstraints) {
                checkConstraints(propertyName, stringValue, derivedFromPrimitiveType, ((PrimitiveDataType) dataType).getConstraints());
            }
        }
    }

    private void checkConstraints(final String propertyName, final String stringValue, final String typeName, List<PropertyConstraint> constraints)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        ConstraintInformation consInformation = null;
        for (PropertyConstraint constraint : constraints) {
            IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(typeName);
            try {
                consInformation = ConstraintUtil.getConstraintInformation(constraint);
                consInformation.setPath(propertyName + ".constraints[" + consInformation.getName() + "]");
                constraint.initialize(toscaType);
                constraint.validate(toscaType, stringValue);
            } catch (ConstraintViolationException e) {
                throw new ConstraintViolationException(e.getMessage(), e, consInformation);
            } catch (IntrospectionException e) {
                // ConstraintValueDoNotMatchPropertyTypeException is not supposed to be raised here (only in constraint definition validation)
                log.info("Constraint introspection error for property <" + propertyName + "> value <" + stringValue + ">", e);
                throw new ConstraintTechnicalException("Constraint introspection error for property <" + propertyName + "> value <" + stringValue + ">", e);
            }
        }
    }

    private void checkDataTypePropertyConstraint(String propertyName, Map<String, Object> complexPropertyValue, PropertyDefinition propertyDefinition,
            Consumer<String> missingPropertyConsumer) throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        DataType dataType = ToscaContext.get(DataType.class, propertyDefinition.getType());
        if (dataType == null) {
            throw new ConstraintViolationException(
                    "Complex type " + propertyDefinition.getType() + " is not complex or it cannot be found in the archive nor in Alien");
        }
        for (Map.Entry<String, Object> complexPropertyValueEntry : complexPropertyValue.entrySet()) {
            if (!safe(dataType.getProperties()).containsKey(complexPropertyValueEntry.getKey())) {
                throw new ConstraintViolationException("Complex type " + propertyDefinition.getType() + " do not have nested property with name "
                        + complexPropertyValueEntry.getKey() + " for property " + propertyName);
            } else {
                Object nestedPropertyValue = complexPropertyValueEntry.getValue();
                PropertyDefinition nestedPropertyDefinition = dataType.getProperties().get(complexPropertyValueEntry.getKey());
                checkPropertyConstraint(propertyName + "." + complexPropertyValueEntry.getKey(), nestedPropertyValue, nestedPropertyDefinition,
                        missingPropertyConsumer);
            }
        }
        // check if the data type has required missing properties
        if (missingPropertyConsumer != null) {
            for (Map.Entry<String, PropertyDefinition> dataTypeDefinition : safe(dataType.getProperties()).entrySet()) {
                if (dataTypeDefinition.getValue().isRequired() && !complexPropertyValue.containsKey(dataTypeDefinition.getKey())) {
                    // A required property is missing
                    String missingPropertyName = propertyName + "." + dataTypeDefinition.getKey();
                    missingPropertyConsumer.accept(missingPropertyName);
                }
            }
        }
    }

    private void checkListPropertyConstraint(String propertyName, List<Object> listPropertyValue, PropertyDefinition propertyDefinition,
            Consumer<String> missingPropertyConsumer) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        PropertyDefinition entrySchema = propertyDefinition.getEntrySchema();
        for (int i = 0; i < listPropertyValue.size(); i++) {
            checkPropertyConstraint(propertyName + "[" + String.valueOf(i) + "]", listPropertyValue.get(i), entrySchema, missingPropertyConsumer);
        }
    }

    private void checkMapPropertyConstraint(String propertyName, Map<String, Object> complexPropertyValue, PropertyDefinition propertyDefinition,
            Consumer<String> missingPropertyConsumer) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        PropertyDefinition entrySchema = propertyDefinition.getEntrySchema();
        for (Map.Entry<String, Object> complexPropertyValueEntry : complexPropertyValue.entrySet()) {
            checkPropertyConstraint(propertyName + "." + complexPropertyValueEntry.getKey(), complexPropertyValueEntry.getValue(), entrySchema,
                    missingPropertyConsumer);
        }
    }

    /**
     * Verify that a complex property value correspond to its definition of constraints
     *
     * @param propertyName name of the property
     * @param complexPropertyValue the value
     * @param propertyDefinition the definition
     * @throws ConstraintViolationException
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     */
    private void checkComplexPropertyConstraint(String propertyName, Map<String, Object> complexPropertyValue, PropertyDefinition propertyDefinition,
            Consumer<String> missingPropertyConsumer) throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        if (ToscaType.MAP.equals(propertyDefinition.getType())) {
            checkMapPropertyConstraint(propertyName, complexPropertyValue, propertyDefinition, missingPropertyConsumer);
        } else {
            checkDataTypePropertyConstraint(propertyName, complexPropertyValue, propertyDefinition, missingPropertyConsumer);
        }
    }

    /**
     * Check that a given value is matching the native type defined on the property definition.
     *
     * @param propertyName The name of the property under validation
     * @param primitiveType The primitive type to check the value against.
     * @param propertyValue The value to check.
     * @throws ConstraintValueDoNotMatchPropertyTypeException in case the value does not match the primitive type.
     */
    private void checkBasicType(final String propertyName, final String primitiveType, final String propertyValue)
            throws ConstraintValueDoNotMatchPropertyTypeException {
        // check basic type value : "boolean" (not handled, no exception thrown)
        // "string" (basic case, no exception), "float", "integer", "version"
        try {
            switch (primitiveType) {
            case "integer":
                Integer.parseInt(propertyValue);
                break;
            case "float":
                Float.parseFloat(propertyValue);
                break;
            case "version":
                VersionUtil.parseVersion(propertyValue);
                break;
            default:
                // last type "string" can have any format
                break;
            }
        } catch (NumberFormatException | InvalidVersionException e) {
            log.debug("The property value for property {} is not of type {}: {}", propertyName, primitiveType, propertyValue, e);
            ConstraintInformation consInformation = new ConstraintInformation(propertyName, null, propertyValue, primitiveType);
            throw new ConstraintValueDoNotMatchPropertyTypeException(e.getMessage(), e, consInformation);
        }
    }
}
