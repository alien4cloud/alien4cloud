package alien4cloud.exception;

public class EnvironmentNotDeployedException extends TechnicalException {

    public EnvironmentNotDeployedException(String message) {
        super(message);
    }

    public EnvironmentNotDeployedException(String message, Throwable cause) {
        super(message, cause);
    }

}
