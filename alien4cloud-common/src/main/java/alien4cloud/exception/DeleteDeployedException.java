package alien4cloud.exception;

/**
 * Thrown when a deployed environment / topology is about to be deleted
 * 
 * @author mourouvi
 *
 */
public class DeleteDeployedException extends TechnicalException {

    private static final long serialVersionUID = -5192834855057177252L;

    public DeleteDeployedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeleteDeployedException(String message) {
        super(message);
    }
}
