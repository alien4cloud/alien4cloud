package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.ToscaType;

public class ToscaPropertyDefaultValueTypeValidator implements ConstraintValidator<ToscaPropertyDefaultValueType, PropertyDefinition> {

    @Override
    public void initialize(ToscaPropertyDefaultValueType constraintAnnotation) {
    }

    @Override
    public boolean isValid(PropertyDefinition value, ConstraintValidatorContext context) {
        PropertyValue defaultValue = value.getDefault();
        if (defaultValue == null) {
            // no default value is specified.
            return true;
        }
        if (!(defaultValue instanceof ScalarPropertyValue)) {
            // No constraint can be made on other thing than scalar values
            return false;
        }
        IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(value.getType());

        if (toscaType == null) {
            return false;
        }
        try {
            toscaType.parse(((ScalarPropertyValue) defaultValue).getValue());
        } catch (InvalidPropertyValueException e) {
            return false;
        }
        return true;
    }
}