package org.alien4cloud.tosca.editor.exception;

/**
 * Exception in case of an IO error in the editor.
 */
public class EditorIOException extends RuntimeException {
    /**
     * Constructor.
     * 
     * @param message Message.
     * @param e Root exception.
     */
    public EditorIOException(String message, Exception e) {
        super(message, e);
    }
}
