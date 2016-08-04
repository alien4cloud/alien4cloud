package org.alien4cloud.tosca.editor.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Exception triggered when the path specified in an upload file operation is not valid.
 */
public class InvalidPathException extends TechnicalException {

    /**
     * Create a new InvalidPathException with an explanation message.
     * 
     * @param message Explanation message.
     */
    public InvalidPathException(String message) {
        super(message);
    }
}