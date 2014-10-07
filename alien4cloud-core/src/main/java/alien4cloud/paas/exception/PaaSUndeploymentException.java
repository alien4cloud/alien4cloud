package alien4cloud.paas.exception;

public class PaaSUndeploymentException extends PaaSTechnicalException {

    private static final long serialVersionUID = -6812501516612307525L;

    public PaaSUndeploymentException(String message) {
        super(message);
    }

    public PaaSUndeploymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
