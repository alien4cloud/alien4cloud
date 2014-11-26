package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import alien4cloud.tosca.model.PropertyConstraint;
import alien4cloud.tosca.model.ToscaType;
import alien4cloud.tosca.model.PropertyDefinition;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

public class ToscaPropertyDefaultValueConstraintsValidator implements ConstraintValidator<ToscaPropertyDefaultValueConstraints, PropertyDefinition> {

    @Override
    public void initialize(ToscaPropertyDefaultValueConstraints constraintAnnotation) {
    }

    @Override
    public boolean isValid(PropertyDefinition value, ConstraintValidatorContext context) {
        // validate that the default value matches the defined constraints.
        ToscaType toscaType = ToscaType.fromYamlTypeName(value.getType());
        if (toscaType == null) {
            return false;
        }

        String defaultAsString = value.getDefault();
        if (defaultAsString == null) {
            // no default value is specified.
            return true;
        }

        Object defaultValue = toscaType.convert(defaultAsString);
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