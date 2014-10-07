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
public class LengthConstraint extends AbstractStringPropertyConstraint {

    @JsonDeserialize(using = NumberDeserializer.class)
    @NotNull
    private Integer length;

    @Override
    protected void doValidate(String propertyValue) throws ConstraintViolationException {
        if (propertyValue.length() != length) {
            throw new ConstraintViolationException("The length of the value is not equals to [" + length + "]");
        }
    }
}