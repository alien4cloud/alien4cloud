package alien4cloud.paas.exception;

public class PaaSDeploymentException extends PaaSTechnicalException {

    private static final long serialVersionUID = -7285334798897608882L;

    public PaaSDeploymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaaSDeploymentException(String message) {

        super(message);
    }
}
