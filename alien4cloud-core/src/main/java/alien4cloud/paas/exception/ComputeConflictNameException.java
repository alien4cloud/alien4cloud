package alien4cloud.paas.exception;


public class ComputeConflictNameException extends PaaSTechnicalException {

    private static final long serialVersionUID = 1L;

    public ComputeConflictNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComputeConflictNameException(String message) {
        super(message);
    }

}
