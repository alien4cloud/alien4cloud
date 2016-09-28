package org.alien4cloud.tosca.model.definitions.constraints;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.json.deserializer.TextDeserializer;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = { "lessOrEqual" })
@SuppressWarnings({ "unchecked" })
public class LessOrEqualConstraint extends AbstractComparablePropertyConstraint {

    @JsonDeserialize(using = TextDeserializer.class)
    @NotNull
    private String lessOrEqual;

    @Override
    public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        initialize(lessOrEqual, propertyType);
    }

    @Override
    protected void doValidate(Object propertyValue) throws ConstraintViolationException {
        if (getComparable().compareTo(propertyValue) < 0) {
            throw new ConstraintViolationException(propertyValue + " >= " + lessOrEqual);
        }
    }
}