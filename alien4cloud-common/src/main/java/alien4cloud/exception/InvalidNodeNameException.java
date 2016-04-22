package alien4cloud.exception;

/**
 * Exception to be thrown when trying to create or rename a node template with an invalid name.
 * 
 */
public class InvalidNodeNameException extends TechnicalException {
    private static final long serialVersionUID = -6151150122897145634L;

    /**
     * Create a new {@link InvalidNodeNameException} with the cause.
     *
     * @param message Message.
     */
    public InvalidNodeNameException(String message) {
        super(message);
    }
}