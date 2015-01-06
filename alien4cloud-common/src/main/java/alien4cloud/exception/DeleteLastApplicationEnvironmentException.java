package alien4cloud.exception;


public class DeleteLastApplicationEnvironmentException extends TechnicalException {

    private static final long serialVersionUID = -5192834855057177252L;

    public DeleteLastApplicationEnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeleteLastApplicationEnvironmentException(String message) {
        super(message);
    }
}
