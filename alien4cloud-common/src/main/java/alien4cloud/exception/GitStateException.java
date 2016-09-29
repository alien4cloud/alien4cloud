package alien4cloud.exception;

/**
 * Exception to be thrown in case there is an issue with a git repository.
 */
public class GitStateException extends GitException {
    private static final long serialVersionUID = -5917605742879793241L;

    public GitStateException(String message, String state) {
        super(String.format("%s (state=%s)", message, state));
    }

    public GitStateException(String message, String state, Throwable cause) {
        super(String.format("%s (state=%s)", message, state), cause);
    }
}