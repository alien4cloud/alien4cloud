package org.alien4cloud.tosca.model.definitions;

import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

public interface PropertyConstraint {

    /**
     * Initialize the property constraint from the constraint value (un-typed from parsing) with the property type.
     *
     * @param propertyType The type of the property on which the constraint is applied (integer, double, string etc.)
     * @throws ConstraintValueDoNotMatchPropertyTypeException In case the specified value doesn't matches the
     */
    void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException;

    /**
     * Validate that the given property value matches the constraint.
     *
     * @param propertyValue The property value.
     * @throws ConstraintViolationException In case the property value doesn't matches the constraint.
     */
    void validate(Object propertyValue) throws ConstraintViolationException;

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