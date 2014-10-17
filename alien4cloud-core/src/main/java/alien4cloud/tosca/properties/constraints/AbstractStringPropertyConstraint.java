package alien4cloud.tosca.properties.constraints;

import alien4cloud.tosca.container.model.type.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

public abstract class AbstractStringPropertyConstraint extends AbstractPropertyConstraint {

    protected abstract void doValidate(String propertyValue) throws ConstraintViolationException;

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            throw new ConstraintViolationException("Value to validate is null");
        }
        if (!(propertyValue instanceof String)) {
            throw new ConstraintViolationException("This constraint can only be applied on String value");
        }
        doValidate((String) propertyValue);
    }

    @Override
    public void initialize(ToscaType propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        ConstraintUtil.checkStringType(propertyType);
    }
}
