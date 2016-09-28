package alien4cloud.tosca.container.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;

@Slf4j
public class ToscaPropertyConstraintValidator implements ConstraintValidator<ToscaPropertyConstraint, PropertyDefinition> {

    @Override
    public void initialize(ToscaPropertyConstraint constraintAnnotation) {
    }

    @Override
    public boolean isValid(PropertyDefinition value, ConstraintValidatorContext context) {
        if (value.getConstraints() == null) {
            return true;
        }
        IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(value.getType());
        if (toscaType == null) {
            return false;
        }
        boolean isValid = true;
        for (int i = 0; i < value.getConstraints().size(); i++) {
            PropertyConstraint constraint = value.getConstraints().get(i);
            try {
                constraint.initialize(toscaType);
            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                log.info("Constraint definition error", e);
                context.buildConstraintViolationWithTemplate("CONSTRAINTS.VALIDATION.TYPE").addPropertyNode("constraints").addBeanNode().inIterable()
                        .atIndex(i).addConstraintViolation();
                isValid = false;
            }
        }
        return isValid;
    }
}