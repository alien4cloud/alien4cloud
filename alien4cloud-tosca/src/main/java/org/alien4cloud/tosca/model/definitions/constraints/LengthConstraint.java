package org.alien4cloud.tosca.model.definitions.constraints;

import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = { "length" })
public class LengthConstraint extends AbstractStringPropertyConstraint {
    @NotNull
    private Integer length;

    @Override
    protected void doValidate(String propertyValue) throws ConstraintViolationException {
        if (propertyValue.length() != length) {
            throw new ConstraintViolationException("The length of the value is not equals to [" + length + "]");
        }
    }
}