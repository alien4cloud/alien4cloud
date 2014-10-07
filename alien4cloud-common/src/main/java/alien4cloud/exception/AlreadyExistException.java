package alien4cloud.exception;

/**
 * Exception to be thrown when trying to insert an existing an element that already exists.
 * 
 * @author luc boutier
 */
public class AlreadyExistException extends TechnicalException {
    private static final long serialVersionUID = -6151150122897145634L;

    /**
     * Create a new {@link AlreadyExistException} with the cause.
     * 
     * @param message Message.
     */
    public AlreadyExistException(String message) {
        super(message);
    }
}