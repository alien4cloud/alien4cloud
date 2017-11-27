package org.alien4cloud.tosca.editor.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Exception triggered when a secret is set on a forbidden property or capability.
 */
public class UnsupportedSecretException extends TechnicalException {

    /**
     * Create a new InvalidPathException with an explanation message.
     *
     * @param message Explanation message.
     */
    public UnsupportedSecretException(String message) {
        super(message);
    }
}