package org.alien4cloud.tosca.model.definitions.constraints;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.normative.types.IPropertyType;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;

/**
 * Defines a property constraint that can be used for matching purpose.
 */
public interface IMatchPropertyConstraint extends PropertyConstraint {
    /**
     * Initialize the property constraint from the constraint value (un-typed from parsing) with the property type.
     *
     * @param textValue The value to configure the constraint.
     * @throws ConstraintValueDoNotMatchPropertyTypeException In case the specified value doesn't matches the
     */
    void setConstraintValue(IPropertyType<?> toscaType, String textValue) throws ConstraintValueDoNotMatchPropertyTypeException;
}
