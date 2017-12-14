package org.alien4cloud.secret.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Secret provider plugin throw this exception when the authentication fails
 */
public class AuthenticationException extends TechnicalException {

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationException(String message) {
        super(message);
    }
}
