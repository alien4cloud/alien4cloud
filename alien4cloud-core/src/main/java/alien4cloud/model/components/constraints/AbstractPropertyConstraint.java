package alien4cloud.model.components.constraints;

import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

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