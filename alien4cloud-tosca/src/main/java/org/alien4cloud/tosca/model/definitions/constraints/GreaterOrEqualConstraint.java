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
@EqualsAndHashCode(callSuper = false, of = { "greaterOrEqual" })
@SuppressWarnings({ "unchecked" })
public class GreaterOrEqualConstraint extends AbstractComparablePropertyConstraint {
    @NotNull
    private String greaterOrEqual;

    @Override
    public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        initialize(greaterOrEqual, propertyType);
    }

    @Override
    protected void doValidate(Object propertyValue) throws ConstraintViolationException {
        if (getComparable().compareTo(propertyValue) > 0) {
            throw new ConstraintViolationException(propertyValue + " <= " + greaterOrEqual);
        }
    }
}