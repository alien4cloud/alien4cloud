package alien4cloud.exception;

public class GitNotAuthorizedException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -5917605742879793240L;
    public GitNotAuthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitNotAuthorizedException(String message) {
        super(message);
    }
}