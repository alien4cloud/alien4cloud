package alien4cloud.exception;

public class GitCloneUriException extends Exception {


    private static final long serialVersionUID = -808476665414269207L;

    public GitCloneUriException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitCloneUriException(String message) {
        super(message);
    }
}
