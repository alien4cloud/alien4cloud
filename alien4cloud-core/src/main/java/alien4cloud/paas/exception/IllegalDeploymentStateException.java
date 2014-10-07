package alien4cloud.paas.exception;

public class IllegalDeploymentStateException extends PaaSTechnicalException {

    private static final long serialVersionUID = -314151651737591239L;

    public IllegalDeploymentStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalDeploymentStateException(String message) {
        super(message);
    }
}
