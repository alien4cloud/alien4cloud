package org.alien4cloud.tosca.model.definitions.constraints;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

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