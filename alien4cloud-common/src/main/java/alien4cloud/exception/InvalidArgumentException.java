package alien4cloud.exception;

public class InvalidArgumentException extends TechnicalException {

    /**
     * 
     */
    private static final long serialVersionUID = 931646037604062840L;

    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidArgumentException(String message) {
        super(message);
    }
}
