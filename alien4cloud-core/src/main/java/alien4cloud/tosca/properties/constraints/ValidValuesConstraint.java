package alien4cloud.tosca.properties.constraints;

import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.model.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.ui.form.annotation.FormProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;

@Getter
@Setter
@FormProperties("validValues")
@EqualsAndHashCode(callSuper = false, of = { "validValues" })
public class ValidValuesConstraint extends AbstractPropertyConstraint {
    @NotNull
    private List<String> validValues;
    @JsonIgnore
    private Set<Object> validValuesTyped;

    @Override
    public void initialize(ToscaType propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        validValuesTyped = Sets.newHashSet();
        if (validValues == null) {
            throw new ConstraintValueDoNotMatchPropertyTypeException("validValues constraint has invalid value <> property type is <" + propertyType.toString()
                    + ">");
        }
        for (String value : validValues) {
            if (!propertyType.isValidValue(value)) {
                throw new ConstraintValueDoNotMatchPropertyTypeException("validValues constraint has invalid value <" + value + "> property type is <"
                        + propertyType.toString() + ">");
            } else {
                validValuesTyped.add(propertyType.convert(value));
            }
        }
    }

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            throw new ConstraintViolationException("Value to validate is null");
        }
        if (!validValuesTyped.contains(propertyValue)) {
            throw new ConstraintViolationException("The value is not in the list of valid values");
        }
    }
}
