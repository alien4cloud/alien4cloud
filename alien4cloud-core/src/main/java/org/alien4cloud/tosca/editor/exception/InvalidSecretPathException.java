package org.alien4cloud.tosca.editor.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Exception triggered when a secret path is not valid.
 */
public class InvalidSecretPathException extends TechnicalException {

    /**
     * Create a new InvalidSecretPathException with an explanation message.
     *
     * @param message Explanation message.
     */
    public InvalidSecretPathException(String message) {
        super(message);
    }
}