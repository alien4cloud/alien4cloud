package org.alien4cloud.secret.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Secret provider plugin throw this exception when an unknown authentication method is encountered
 */
public class NotSupportedAuthenticationMethod extends TechnicalException {

    public NotSupportedAuthenticationMethod(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupportedAuthenticationMethod(String message) {
        super(message);
    }
}
