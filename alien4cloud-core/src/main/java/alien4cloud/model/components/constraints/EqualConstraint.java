package alien4cloud.model.components.constraints;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = { "equal" })
public class EqualConstraint extends AbstractPropertyConstraint implements IMatchPropertyConstraint {
    @NotNull
    private String equal;

    @JsonIgnore
    private Object typed;

    @Override
    public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        typed = ConstraintUtil.convert(propertyType, equal);
    }

    @Override
    public void setConstraintValue(IPropertyType<?> toscaType, String textValue) throws ConstraintValueDoNotMatchPropertyTypeException {
        equal = textValue;
        typed = ConstraintUtil.convert(toscaType, textValue);
    }

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            if (typed != null) {
                fail(propertyValue);
            }
        } else if (typed == null) {
            fail(propertyValue);
        } else if (!typed.equals(propertyValue)) {
            fail(propertyValue);
        }
    }

    private void fail(Object propertyValue) throws ConstraintViolationException {
        throw new ConstraintViolationException(
                "Equal constraint violation, the reference is <" + equal + "> but the value to compare is <" + propertyValue + ">");
    }
}