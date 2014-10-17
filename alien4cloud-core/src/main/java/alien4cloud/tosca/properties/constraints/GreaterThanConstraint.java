package alien4cloud.tosca.properties.constraints;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.container.deserializer.TextDeserializer;
import alien4cloud.tosca.container.model.type.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@SuppressWarnings({ "PMD.UnusedPrivateField", "unchecked" })
public class GreaterThanConstraint extends AbstractComparablePropertyConstraint {

    @JsonDeserialize(using = TextDeserializer.class)
    @NotNull
    private String greaterThan;

    @Override
    public void initialize(ToscaType propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        initialize(greaterThan, propertyType);
    }

    @Override
    protected void doValidate(Object propertyValue) throws ConstraintViolationException {
        if (getComparable().compareTo(propertyValue) >= 0) {
            throw new ConstraintViolationException(propertyValue + " < " + greaterThan);
        }
    }
}
