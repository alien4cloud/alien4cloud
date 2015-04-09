package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.ToscaType;

public class ToscaPropertyDefaultValueTypeValidator implements ConstraintValidator<ToscaPropertyDefaultValueType, PropertyDefinition> {

    @Override
    public void initialize(ToscaPropertyDefaultValueType constraintAnnotation) {
    }

    @Override
    public boolean isValid(PropertyDefinition value, ConstraintValidatorContext context) {
        IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(value.getType());

        if (toscaType == null) {
            return false;
        }

        String defaultAsString = value.getDefault();
        if (defaultAsString == null) {
            // no default value is specified.
            return true;
        }

        try {
            toscaType.parse(defaultAsString);
        } catch (InvalidPropertyValueException e) {
            return false;
        }
        return true;
    }
}