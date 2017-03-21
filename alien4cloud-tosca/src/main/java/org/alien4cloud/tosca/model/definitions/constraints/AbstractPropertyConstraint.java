package org.alien4cloud.tosca.model.definitions.constraints;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.normative.types.IPropertyType;
import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;

public abstract class AbstractPropertyConstraint implements PropertyConstraint {

    @Override
    public void validate(IPropertyType<?> toscaType, String propertyTextValue) throws ConstraintViolationException {
        try {
            validate(toscaType.parse(propertyTextValue));
        } catch (InvalidPropertyValueException e) {
            throw new ConstraintViolationException("String value [" + propertyTextValue + "] is not valid for type [" + toscaType.getTypeName() + "]", e);
        }
    }
}