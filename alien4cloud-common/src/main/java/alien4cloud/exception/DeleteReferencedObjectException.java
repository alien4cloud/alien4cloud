package alien4cloud.exception;

/**
 * This exception should be thrown when attempting to delete an object in Alien which is still referenced (used) by other object
 *
 * @author Minh Khang VU
 */
public class DeleteReferencedObjectException extends TechnicalException {

    public DeleteReferencedObjectException(String message) {
        super(message);
    }

    public DeleteReferencedObjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
