package org.alien4cloud.tosca.exceptions;

/**
 * Exception thrown when the user defines invalid custom property constraint
 * 
 * @author mkv
 * 
 */
public class InvalidPropertyConstraintImplementationException extends ConstraintTechnicalException {

    private static final long serialVersionUID = 2797550944328544706L;

    public InvalidPropertyConstraintImplementationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPropertyConstraintImplementationException(String message) {
        super(message);
    }
}
