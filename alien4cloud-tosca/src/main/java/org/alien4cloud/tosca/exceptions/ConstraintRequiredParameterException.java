package org.alien4cloud.tosca.exceptions;

import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;

/**
 * Exception to be thrown when a required property is not provided
 * 
 * @author mourouvi
 *
 */
public class ConstraintRequiredParameterException extends ConstraintFunctionalException {

    private static final long serialVersionUID = 1L;

    public ConstraintRequiredParameterException(String message) {
        super(message);
    }

    public ConstraintRequiredParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintRequiredParameterException(String message, Throwable cause, ConstraintInformation constraintInformation) {
        super(message, cause);
        this.constraintInformation = constraintInformation;
    }

}
