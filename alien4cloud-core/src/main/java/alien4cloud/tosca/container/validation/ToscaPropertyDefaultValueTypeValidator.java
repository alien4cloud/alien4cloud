package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import alien4cloud.tosca.container.model.type.ToscaType;
import alien4cloud.tosca.model.PropertyDefinition;

public class ToscaPropertyDefaultValueTypeValidator implements ConstraintValidator<ToscaPropertyDefaultValueType, PropertyDefinition> {

    @Override
    public void initialize(ToscaPropertyDefaultValueType constraintAnnotation) {
    }

    @Override
    public boolean isValid(PropertyDefinition value, ConstraintValidatorContext context) {
        ToscaType toscaType = ToscaType.fromYamlTypeName(value.getType());

        String defaultAsString = value.getDefault();
        if (defaultAsString == null) {
            // no default value is specified.
            return true;
        }

        if (toscaType == null) {
            return false;
        }

        return toscaType.isValidValue(defaultAsString);
    }
}