package alien4cloud.tosca.properties.constraints;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.container.deserializer.NumberDeserializer;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@SuppressWarnings({ "PMD.UnusedPrivateField" })
public class MinLengthConstraint extends AbstractStringPropertyConstraint {

    @JsonDeserialize(using = NumberDeserializer.class)
    @NotNull
    private Integer minLength;

    @Override
    protected void doValidate(String propertyValue) throws ConstraintViolationException {
        if (propertyValue.length() < minLength) {
            throw new ConstraintViolationException("The length of the value is less than [" + minLength + "]");
        }
    }
}
