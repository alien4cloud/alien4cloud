package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import alien4cloud.tosca.container.model.type.ToscaType;

public class ToscaPropertyTypeValidator implements ConstraintValidator<ToscaPropertyType, String> {

    @Override
    public void initialize(ToscaPropertyType constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (ToscaType.fromYamlTypeName(value) == null) {
            return false;
        }
        return true;
    }
}