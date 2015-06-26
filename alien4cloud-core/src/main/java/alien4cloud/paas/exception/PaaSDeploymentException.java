package alien4cloud.paas.exception;

import alien4cloud.rest.model.RestErrorCode;

public class PaaSDeploymentException extends PaaSTechnicalException {

    private static final long serialVersionUID = -7285334798897608882L;
    RestErrorCode restErrorCode = null;

    public PaaSDeploymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaaSDeploymentException(String message) {
        super(message);
    }

    public PaaSDeploymentException(String message, RestErrorCode passErrorCode) {
        super(message);
        this.restErrorCode = passErrorCode;
    }

    public RestErrorCode getPassErrorCode() {
        return this.restErrorCode;
    }
}
