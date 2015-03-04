package alien4cloud.model.components.constraints;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = { "greaterThan" })
@SuppressWarnings({ "PMD.UnusedPrivateField", "unchecked" })
public class GreaterThanConstraint extends AbstractComparablePropertyConstraint {
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

    @Override
    public boolean isCompatible(PropertyConstraint propertyConstraint) {
        if ((propertyConstraint instanceof GreaterThanConstraint)
                && this.getGreaterThan().equals(((GreaterThanConstraint) propertyConstraint).getGreaterThan())) {
            return true;
        }
        return false;
    }
}
