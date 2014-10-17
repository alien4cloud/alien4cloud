package alien4cloud.tosca.container.model.type;

import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

public interface PropertyConstraint {

    void initialize(ToscaType propertyType) throws ConstraintValueDoNotMatchPropertyTypeException;

    void validate(Object propertyValue) throws ConstraintViolationException;

    void validate(ToscaType toscaType, String propertyTextValue) throws ConstraintViolationException;
}