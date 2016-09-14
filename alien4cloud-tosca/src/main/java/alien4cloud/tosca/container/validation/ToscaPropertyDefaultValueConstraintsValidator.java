package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
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
        PropertyValue defaultValue = value.getDefault();
        if (defaultValue == null) {
            // no default value is specified.
            return true;
        }
        // validate that the default value matches the defined constraints.
        IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(value.getType());
        if (toscaType == null) {
            return false;
        }
        if (!(defaultValue instanceof ScalarPropertyValue)) {
            // No constraint can be made on other thing than scalar values
            return false;
        }
        String defaultValueAsString = ((ScalarPropertyValue) defaultValue).getValue();
        Object parsedDefaultValue;
        try {
            parsedDefaultValue = toscaType.parse(defaultValueAsString);
        } catch (InvalidPropertyValueException e) {
            return false;
        }
        if (value.getConstraints() != null) {
            for (PropertyConstraint constraint : value.getConstraints()) {
                try {
                    constraint.validate(parsedDefaultValue);
                } catch (ConstraintViolationException e) {
                    return false;
                }
            }
        }
        return true;
    }
}