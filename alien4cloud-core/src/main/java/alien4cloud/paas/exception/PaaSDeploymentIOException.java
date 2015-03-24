package alien4cloud.paas.exception;

public class PaaSDeploymentIOException extends PaaSTechnicalException {

    /**
     *
     */
    private static final long serialVersionUID = 2608253562233425513L;

    public PaaSDeploymentIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaaSDeploymentIOException(String message) {
        super(message);
    }
}
