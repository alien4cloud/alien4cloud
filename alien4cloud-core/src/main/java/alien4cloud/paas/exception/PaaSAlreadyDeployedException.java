package alien4cloud.paas.exception;

public class PaaSAlreadyDeployedException extends PaaSDeploymentException {

    private static final long serialVersionUID = 9214713773651689579L;

    public PaaSAlreadyDeployedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaaSAlreadyDeployedException(String message) {
        super(message);
    }

}
