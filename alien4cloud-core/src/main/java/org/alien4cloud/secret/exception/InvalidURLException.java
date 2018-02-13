package org.alien4cloud.secret.exception;

public class InvalidURLException extends SecretProviderException {

    public InvalidURLException(String message) {
        super(message);
    }

    public InvalidURLException(String message, Throwable cause) {
        super(message, cause);
    }
}
