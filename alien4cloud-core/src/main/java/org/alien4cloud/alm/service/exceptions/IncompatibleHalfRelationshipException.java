package org.alien4cloud.alm.service.exceptions;

import alien4cloud.exception.TechnicalException;

/**
 * Exception thrown when trying to attach an half relationship to a service
 */
public class IncompatibleHalfRelationshipException extends TechnicalException {

    public IncompatibleHalfRelationshipException(String message) {
        super(message);
    }
}
