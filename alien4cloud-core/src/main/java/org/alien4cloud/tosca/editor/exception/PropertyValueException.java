package org.alien4cloud.tosca.editor.exception;

import org.alien4cloud.tosca.exceptions.ConstraintFunctionalException;
import lombok.Getter;

/**
 * Wraps a ConstraintValueDoNotMatchPropertyTypeException or a ConstraintViolationException and adds the property name and value information.
 */
@Getter
public class PropertyValueException extends RuntimeException {
    private String propertyName;
    private Object propertyValue;

    /**
     * Create a new PVE exception from the various informations
     * 
     * @param message A technical message.
     * @param cause The cause of the error when checking or setting the property value.
     * @param propertyName The name of the property.
     * @param propertyValue The value of the property.
     */
    public PropertyValueException(String message, ConstraintFunctionalException cause, String propertyName, Object propertyValue) {
        super(message, cause);
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }
}
