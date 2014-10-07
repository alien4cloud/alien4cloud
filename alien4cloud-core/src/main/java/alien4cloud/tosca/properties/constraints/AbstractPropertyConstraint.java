package alien4cloud.tosca.properties.constraints;

import alien4cloud.tosca.container.model.type.PropertyConstraint;
import alien4cloud.tosca.container.model.type.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.version.InvalidVersionException;

public abstract class AbstractPropertyConstraint implements PropertyConstraint {

    @Override
    public void validate(ToscaType toscaType, String propertyTextValue) throws ConstraintViolationException {
        try {
            validate(toscaType.convert(propertyTextValue));
        } catch (IllegalArgumentException | InvalidVersionException e) {
            throw new ConstraintViolationException("String value [" + propertyTextValue + "] is not valid for type [" + toscaType + "]", e);
        }
    }
}
