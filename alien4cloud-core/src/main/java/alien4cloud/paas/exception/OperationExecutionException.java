package alien4cloud.paas.exception;


public class OperationExecutionException extends PaaSTechnicalException {

    private static final long serialVersionUID = 1L;

    public OperationExecutionException(String message, Throwable cause) {
        super(message, cause);

    }

    public OperationExecutionException(String message) {
        super(message);
    }

}
