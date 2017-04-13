package org.alien4cloud.alm.service.exceptions;

import alien4cloud.exception.TechnicalException;

/**
 * Exception to be thrown when trying to create a service out of a topology with no substitution.
 */
public class MissingSubstitutionException extends TechnicalException {
    public MissingSubstitutionException(String message) {
        super(message);
    }
}