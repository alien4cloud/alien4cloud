package org.alien4cloud.secret.exception;

/**
 * Secret provider plugin throw this exception when the authentication fails
 */
public class SecretProviderAuthenticationException extends SecretProviderException {

    public SecretProviderAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecretProviderAuthenticationException(String message) {
        super(message);
    }
}
