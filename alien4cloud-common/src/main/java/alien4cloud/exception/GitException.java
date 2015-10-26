package alien4cloud.exception;

/**
 * Exception to be thrown in case there is an issue with a git repository.
 */
public class GitException extends TechnicalException {
    private static final long serialVersionUID = -5917605742879793240L;

    public GitException(String message) {
        super(message);
    }

    public GitException(String message, Throwable cause) {
        super(message, cause);
    }
}