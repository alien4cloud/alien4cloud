package org.alien4cloud.tosca.exceptions;

import alien4cloud.exception.TechnicalException;

/**
 * Base class for all constraint related exceptions
 * 
 * @author mkv
 * 
 */
public class ConstraintTechnicalException extends TechnicalException {

    private static final long serialVersionUID = 5829360730980521567L;

    public ConstraintTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstraintTechnicalException(String message) {
        super(message);
    }

}
