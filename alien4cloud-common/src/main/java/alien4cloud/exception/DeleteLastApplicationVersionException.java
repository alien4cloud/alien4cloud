package alien4cloud.exception;


public class DeleteLastApplicationVersionException extends TechnicalException {

    private static final long serialVersionUID = -5192834855057177252L;

    public DeleteLastApplicationVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeleteLastApplicationVersionException(String message) {
        super(message);
    }
}
