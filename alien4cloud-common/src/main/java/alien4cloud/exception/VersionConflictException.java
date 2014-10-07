package alien4cloud.exception;

/**
 * Exception happens when a4c cannot resolve version conflicts in a transparent manner for users
 */
public class VersionConflictException extends TechnicalException {

    public VersionConflictException(String message) {
        super(message);
    }

    public VersionConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
