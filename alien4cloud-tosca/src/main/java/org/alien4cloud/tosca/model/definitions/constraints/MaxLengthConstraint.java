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
@EqualsAndHashCode(callSuper = false, of = { "maxLength" })
public class MaxLengthConstraint extends AbstractLengthConstraint {
    @NotNull
    private Integer maxLength;

    public MaxLengthConstraint(Integer maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    protected void doValidate(int propertyValue) throws ConstraintViolationException {
        if (propertyValue > maxLength) {
            throw new ConstraintViolationException("The length of the value is greater than [" + maxLength + "]");
        }
    }
}