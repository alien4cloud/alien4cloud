package org.alien4cloud.secret.exception;

import alien4cloud.exception.TechnicalException;

public abstract class SecretProviderException extends TechnicalException {

    public SecretProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretProviderException(String message) {
        super(message);
    }
}
