package org.alien4cloud.tosca.model.definitions.constraints;

import javax.validation.constraints.NotNull;

import org.alien4cloud.tosca.exceptions.ConstraintViolationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, of = { "minLength" })
public class MinLengthConstraint extends AbstractLengthConstraint {
    @NotNull
    private Integer minLength;

    public MinLengthConstraint(Integer minLength) {
        this.minLength = minLength;
    }

    @Override
    protected void doValidate(int propertyValue) throws ConstraintViolationException {
        if (propertyValue < minLength) {
            throw new ConstraintViolationException("The length of the value is less than [" + minLength + "]");
        }
    }
}
