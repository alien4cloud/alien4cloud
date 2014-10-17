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
public class MaxLengthConstraint extends AbstractStringPropertyConstraint {

    @JsonDeserialize(using = NumberDeserializer.class)
    @NotNull
    private Integer maxLength;

    @Override
    protected void doValidate(String propertyValue) throws ConstraintViolationException {
        if (propertyValue.length() > maxLength) {
            throw new ConstraintViolationException("The length of the value is greater than [" + maxLength + "]");
        }
    }
}