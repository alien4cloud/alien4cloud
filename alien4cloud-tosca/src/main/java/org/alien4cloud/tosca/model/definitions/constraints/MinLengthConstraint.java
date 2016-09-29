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
@EqualsAndHashCode(callSuper = false, of = { "minLength" })
public class MinLengthConstraint extends AbstractStringPropertyConstraint {
    @NotNull
    private Integer minLength;

    public MinLengthConstraint(Integer minLength) {
        this.minLength = minLength;
    }

    @Override
    protected void doValidate(String propertyValue) throws ConstraintViolationException {
        if (propertyValue.length() < minLength) {
            throw new ConstraintViolationException("The length of the value is less than [" + minLength + "]");
        }
    }
}
