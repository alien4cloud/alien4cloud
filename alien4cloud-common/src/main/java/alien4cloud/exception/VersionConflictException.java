package alien4cloud.exception;

/**
 * Exception happens when a4c cannot resolve version conflicts in a transparent manner for users
 */
public class VersionConflictException extends TechnicalException {

    private static final long serialVersionUID = 2266821614841901090L;

    public VersionConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersionConflictException(String message) {
        super(message);
    }

}
