package alien4cloud.tosca.container.validation;

import java.util.List;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;

import com.google.common.collect.Sets;

public class ToscaPropertyConstraintDuplicateValidator implements ConstraintValidator<ToscaPropertyConstraintDuplicate, List<PropertyConstraint>> {

    @Override
    public void initialize(ToscaPropertyConstraintDuplicate constraintAnnotation) {
    }

    @Override
    public boolean isValid(List<PropertyConstraint> value, ConstraintValidatorContext context) {
        if(value == null) {
            return true;
        }
        Set<String> definedConstraints = Sets.newHashSet();
        boolean isValid = true;
        for (int i = 0; i < value.size(); i++) {
            PropertyConstraint constraint = value.get(i);
            if (!definedConstraints.add(constraint.getClass().getName())) {
                context.buildConstraintViolationWithTemplate("CONSTRAINTS.VALIDATION.DUPLICATED_CONSTRAINT").addBeanNode().inIterable()
                        .atIndex(i).addConstraintViolation();
                isValid = false;
            }
        }
        return isValid;
    }
}
