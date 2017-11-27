package org.alien4cloud.tosca.model.definitions.constraints;

import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.normative.types.IPropertyType;

/**
 * Defines a property constraint that can be used for matching purpose.
 */
public interface IMatchPropertyConstraint {
    /**
     * Initialize the property constraint from the constraint value (un-typed from parsing) with the property type.
     *
     * @param textValue The value to configure the constraint.
     * @throws ConstraintValueDoNotMatchPropertyTypeException In case the specified value doesn't matches the
     */
    void setConstraintValue(IPropertyType<?> toscaType, String textValue) throws ConstraintValueDoNotMatchPropertyTypeException;

    /**
     * Validate that the given property value matches the constraint. The property values is specified as a String and is converted to the correct type based on
     * the given tosca type.
     *
     * @param toscaType The type of the property.
     * @param propertyTextValue The value of the property as a string.
     * @throws ConstraintViolationException In case the property value doesn't matches the constraint.
     */
    void validate(IPropertyType<?> toscaType, String propertyTextValue) throws ConstraintViolationException;
}
