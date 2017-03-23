package org.alien4cloud.tosca.model.definitions.constraints;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.normative.types.IPropertyType;
import org.alien4cloud.tosca.normative.types.ListType;
import org.alien4cloud.tosca.normative.types.MapType;
import org.alien4cloud.tosca.normative.types.StringType;

/**
 * Constraint that can be applied on length types.
 */
public abstract class AbstractLengthConstraint extends AbstractPropertyConstraint {
    @Override
    public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        if (!(StringType.NAME.equals(propertyType.getTypeName()) || ListType.NAME.equals(propertyType.getTypeName())
                || MapType.NAME.equals(propertyType.getTypeName()))) {
            throw new ConstraintValueDoNotMatchPropertyTypeException(
                    "Only string, map and list property types are accepted but found <" + propertyType.getTypeName() + ">");
        }
    }

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            throw new ConstraintViolationException("Value to validate is null");
        }
        if (propertyValue instanceof String) {
            doValidate(((String) propertyValue).length());
            return;
        }
        if (propertyValue instanceof List) {
            doValidate(((List) propertyValue).size());
            return;
        }
        if (propertyValue instanceof Map) {
            doValidate(((Map) propertyValue).size());
            return;
        }
        throw new ConstraintViolationException("This constraint can only be applied on String, Map or List values");
    }

    protected abstract void doValidate(int size) throws ConstraintViolationException;
}