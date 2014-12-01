package alien4cloud.tosca.parser;

import alien4cloud.exception.TechnicalException;

/**
 * Exception thrown in case of unexpected errors while parsing tosca definitions.
 */
public class ParsingTechnicalException extends TechnicalException {
    private static final long serialVersionUID = 1L;

    /**
     * Create a technical exception with a single explanation message.
     * 
     * @param message The message that explain the context and cause of the exception.
     */
    public ParsingTechnicalException(String message) {
        super(message);
    }

    /**
     * Create a technical exception with an explanation message and the root cause of the error.
     * 
     * @param message The message that explain the context and cause of the exception.
     * @param cause The root cause.
     */
    public ParsingTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
