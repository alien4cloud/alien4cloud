package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.alien4cloud.tosca.normative.types.ToscaTypes;

public class ToscaPropertyTypeValidator implements ConstraintValidator<ToscaPropertyType, String> {

    @Override
    public void initialize(ToscaPropertyType constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (ToscaTypes.fromYamlTypeName(value) == null) {
            return false;
        }
        return true;
    }
}