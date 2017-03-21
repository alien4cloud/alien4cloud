package org.alien4cloud.tosca.exceptions;

import alien4cloud.exception.FunctionalException;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import lombok.Getter;

/**
 * All functional error related to constraint processing must go here
 * 
 * @author mkv
 * 
 */
public class ConstraintFunctionalException extends FunctionalException {

    private static final long serialVersionUID = 1L;

    @Getter
    protected ConstraintInformation constraintInformation;

    public ConstraintFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintFunctionalException(String message) {
        super(message);
    }

    public ConstraintFunctionalException(String message, Throwable cause, ConstraintInformation constraintInformation) {
        super(message, cause);
        this.constraintInformation = constraintInformation;
    }

}
