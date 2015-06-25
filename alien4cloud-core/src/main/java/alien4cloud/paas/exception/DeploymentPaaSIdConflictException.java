package alien4cloud.paas.exception;


public class DeploymentPaaSIdConflictException extends PaaSTechnicalException {

    private static final long serialVersionUID = 1L;

    public DeploymentPaaSIdConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeploymentPaaSIdConflictException(String message) {
        super(message);
    }

}
