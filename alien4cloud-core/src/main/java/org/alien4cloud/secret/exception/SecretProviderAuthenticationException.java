package org.alien4cloud.secret.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Secret provider plugin throw this exception when the authentication fails
 */
public class SecretProviderAuthenticationException extends TechnicalException {

    public SecretProviderAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretProviderAuthenticationException(String message) {
        super(message);
    }
}
