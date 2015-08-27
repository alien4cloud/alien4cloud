package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

public class ToscaPropertyDefaultValueConstraintsValidator implements ConstraintValidator<ToscaPropertyDefaultValueConstraints, PropertyDefinition> {

    @Override
    public void initialize(ToscaPropertyDefaultValueConstraints constraintAnnotation) {
    }

    @Override
    public boolean isValid(PropertyDefinition value, ConstraintValidatorContext context) {
        String defaultAsString = value.getDefault();
        if (defaultAsString == null) {
            // no default value is specified.
            return true;
        }
        // validate that the default value matches the defined constraints.
        IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(value.getType());
        if (toscaType == null) {
            return false;
        }
        Object defaultValue = null;
        try {
            defaultValue = toscaType.parse(defaultAsString);
        } catch (InvalidPropertyValueException e) {
            return false;
        }
        for (PropertyConstraint constraint : value.getConstraints()) {
            try {
                constraint.validate(defaultValue);
            } catch (ConstraintViolationException e) {
                return false;
            }
        }
        return true;
    }
}