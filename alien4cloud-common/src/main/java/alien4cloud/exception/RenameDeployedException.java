package alien4cloud.exception;

public class RenameDeployedException extends TechnicalException {

    private static final long serialVersionUID = 3788881352771897903L;

    public RenameDeployedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RenameDeployedException(String message) {
        super(message);
    }
}
