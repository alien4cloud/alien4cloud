package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import alien4cloud.model.components.PropertyDefinition;

@Deprecated
public class ToscaPropertyDefaultValueTypeValidator implements ConstraintValidator<ToscaPropertyDefaultValueType, PropertyDefinition> {

    @Override
    public void initialize(ToscaPropertyDefaultValueType constraintAnnotation) {
    }

    @Override
    public boolean isValid(PropertyDefinition value, ConstraintValidatorContext context) {
        // TODO:XDE
        // String defaultAsString = value.getDefault();
        // if (defaultAsString == null) {
        // // no default value is specified.
        // return true;
        // }
        // IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(value.getType());
        //
        // if (toscaType == null) {
        // return false;
        // }
        // try {
        // toscaType.parse(defaultAsString);
        // } catch (InvalidPropertyValueException e) {
        // return false;
        // }
        return true;
    }
}