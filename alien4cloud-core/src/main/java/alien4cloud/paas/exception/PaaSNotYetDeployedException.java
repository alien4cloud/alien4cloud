package alien4cloud.paas.exception;

public class PaaSNotYetDeployedException extends PaaSUndeploymentException {

    private static final long serialVersionUID = 5628164176522605657L;

    public PaaSNotYetDeployedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaaSNotYetDeployedException(String message) {
        super(message);
    }

}
