package org.alien4cloud.tosca.model.definitions.constraints;

import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.fasterxml.jackson.annotation.JsonIgnore;

@SuppressWarnings("rawtypes")
public abstract class AbstractComparablePropertyConstraint extends AbstractPropertyConstraint implements IMatchPropertyConstraint {

    private Comparable comparable;

    @JsonIgnore
    protected Comparable getComparable() {
        return comparable;
    }

    protected void initialize(String rawTextValue, IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        // Perform verification that the property type is supported for comparison
        // Check if the text value is valid for the property type
        // Convert the raw text value to a comparable value
        comparable = ConstraintUtil.convertToComparable(propertyType, rawTextValue);
    }

    protected abstract void doValidate(Object propertyValue) throws ConstraintViolationException;

    @Override
    public void setConstraintValue(IPropertyType<?> propertyType, String value) throws ConstraintValueDoNotMatchPropertyTypeException {
        this.comparable = ConstraintUtil.convertToComparable(propertyType, value);
    }

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            throw new ConstraintViolationException("Value to check is null");
        }
        if (!(comparable.getClass().isAssignableFrom(propertyValue.getClass()))) {
            throw new ConstraintViolationException("Value to check is not comparable to reference type, value type [" + propertyValue.getClass()
                    + "], reference type [" + comparable.getClass() + "]");
        }
        doValidate(propertyValue);
    }
}
