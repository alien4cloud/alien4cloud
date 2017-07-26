package org.alien4cloud.tosca.exceptions;

import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;

/**
 * Exception happened while user violated a predefined constraint
 * 
 * @author mkv
 * 
 */
public class ConstraintViolationException extends ConstraintFunctionalException {

    private static final long serialVersionUID = 1L;

    public ConstraintViolationException(String message) {
        super(message);
    }

    public ConstraintViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintViolationException(String message, Throwable cause, ConstraintInformation constraintInformation) {
        super(message, cause);
        this.constraintInformation = constraintInformation;
    }
}