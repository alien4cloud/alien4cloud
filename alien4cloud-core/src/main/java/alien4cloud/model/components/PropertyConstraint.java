package alien4cloud.model.components;

import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

public interface PropertyConstraint {

    void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException;

    void validate(Object propertyValue) throws ConstraintViolationException;

    void validate(IPropertyType<?> toscaType, String propertyTextValue) throws ConstraintViolationException;
}