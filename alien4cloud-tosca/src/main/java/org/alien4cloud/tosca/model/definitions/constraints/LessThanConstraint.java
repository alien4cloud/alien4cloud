package org.alien4cloud.tosca.model.definitions.constraints;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.normative.types.IPropertyType;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = { "lessThan" })
@SuppressWarnings({ "unchecked" })
public class LessThanConstraint extends AbstractComparablePropertyConstraint {
    @NotNull
    private String lessThan;

    @Override
    public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        initialize(lessThan, propertyType);
    }

    @Override
    protected void doValidate(Object propertyValue) throws ConstraintViolationException {
        if (getComparable().compareTo(propertyValue) <= 0) {
            throw new ConstraintViolationException(propertyValue + " > " + lessThan);
        }
    }
}