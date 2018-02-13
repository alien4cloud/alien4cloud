package org.alien4cloud.secret.exception;

/**
 * Secret provider plugin throw this exception when an unknown authentication method is encountered
 */
public class NotSupportedAuthenticationMethodException extends SecretProviderException {

    public NotSupportedAuthenticationMethodException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupportedAuthenticationMethodException(String message) {
        super(message);
    }
}
