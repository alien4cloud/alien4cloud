package org.alien4cloud.tosca.model.definitions.constraints;

import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, of = { "maxLength" })
public class MaxLengthConstraint extends AbstractStringPropertyConstraint {
    @NotNull
    private Integer maxLength;

    public MaxLengthConstraint(Integer maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    protected void doValidate(String propertyValue) throws ConstraintViolationException {
        if (propertyValue.length() > maxLength) {
            throw new ConstraintViolationException("The length of the value is greater than [" + maxLength + "]");
        }
    }
}