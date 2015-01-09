package alien4cloud.model.components.constraints;

import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

@SuppressWarnings("rawtypes")
public abstract class AbstractComparablePropertyConstraint extends AbstractPropertyConstraint {

    private Comparable comparable;

    protected Comparable getComparable() {
        return comparable;
    }

    protected void initialize(String rawTextValue, ToscaType propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        // Perform verification that the property type is supported for comparison
        ConstraintUtil.checkComparableType(propertyType);
        // Check if the text value is valid for the property type
        if (propertyType.isValidValue(rawTextValue)) {
            // Convert the raw text value to a comparable value
            comparable = ConstraintUtil.convertToComparable(propertyType, rawTextValue);
        } else {
            // Invalid value throw exception
            throw new ConstraintValueDoNotMatchPropertyTypeException("The value [" + rawTextValue + "] is not valid for the type [" + propertyType + "]");
        }
    }

    protected abstract void doValidate(Object propertyValue) throws ConstraintViolationException;

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            throw new ConstraintViolationException("Value to check is null");
        }
        if (!(comparable.getClass().isAssignableFrom(propertyValue.getClass()))) {
            throw new ConstraintViolationException("Value to check is not comparable to reference type, value type [" + propertyValue.getClass()
                    + "], reference type [" + comparable.getClass() + "]");
        }
        doValidate(propertyValue);
    }
}
