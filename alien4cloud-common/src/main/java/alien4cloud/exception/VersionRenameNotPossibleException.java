package alien4cloud.exception;


/**
 * This exception should be thrown when a version of something is not possible since it is used in topologies.
 */
public class VersionRenameNotPossibleException extends TechnicalException {
    private static final long serialVersionUID = 1L;

    public VersionRenameNotPossibleException(String message) {
        super(message);
    }

    public VersionRenameNotPossibleException(String message, Throwable cause) {
        super(message, cause);
    }

}
