package org.alien4cloud.tosca.editor.exception;

/**
 * Exception to be thrown when the name of an element doesn't match the required pattern (inputs, node templates etc.).
 */
public class UnmatchedElementPatternException extends RuntimeException {
    public UnmatchedElementPatternException(String message) {
        super(message);
    }
}