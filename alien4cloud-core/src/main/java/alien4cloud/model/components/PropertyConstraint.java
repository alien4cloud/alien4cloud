package alien4cloud.model.components;

import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

public interface PropertyConstraint {

    void initialize(ToscaType propertyType) throws ConstraintValueDoNotMatchPropertyTypeException;

    void validate(Object propertyValue) throws ConstraintViolationException;

    void validate(ToscaType toscaType, String propertyTextValue) throws ConstraintViolationException;

    boolean isCompatible(PropertyConstraint propertyConstraint);
}