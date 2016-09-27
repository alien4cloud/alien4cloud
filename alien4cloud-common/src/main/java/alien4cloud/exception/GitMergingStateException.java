package alien4cloud.exception;

/**
 * Exception to be thrown in case there is an issue with a git repository.
 */
public class GitMergingStateException extends GitStateException {
    private static final long serialVersionUID = -5917605742879793241L;
    private static final String MERGING = "MERGING";

    public GitMergingStateException(String message) {
        super(message, MERGING);
    }

    public GitMergingStateException(String message, Throwable cause) {
        super(message, MERGING, cause);
    }
}