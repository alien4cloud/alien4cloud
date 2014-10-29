package alien4cloud.tosca.properties.constraints;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.container.model.type.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

@Getter
@Setter
@SuppressWarnings({ "PMD.UnusedPrivateField" })
@EqualsAndHashCode(callSuper = false, of = { "equal" })
public class EqualConstraint extends AbstractPropertyConstraint {
    @NotNull
    private String equal;

    private Object asTyped;

    @Override
    public void initialize(ToscaType propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        if (propertyType.isValidValue(equal)) {
            asTyped = propertyType.convert(equal);
        } else {
            throw new ConstraintValueDoNotMatchPropertyTypeException("equal constraint has invalid value <" + equal + "> property type is <"
                    + propertyType.toString() + ">");
        }
    }

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            if (asTyped != null) {
                fail(propertyValue);
            }
        } else if (asTyped == null) {
            fail(propertyValue);
        } else if (!asTyped.equals(propertyValue)) {
            fail(propertyValue);
        }
    }

    private void fail(Object propertyValue) throws ConstraintViolationException {
        throw new ConstraintViolationException("Equal constraint violation, the reference is <" + equal + "> but the value to compare is <" + propertyValue
                + ">");
    }
}