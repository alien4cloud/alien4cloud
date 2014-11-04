package alien4cloud.application;

import alien4cloud.exception.TechnicalException;

/**
 * Thrown when the deployment setup of a topology is invalid which make it impossible to deploy
 * 
 * @author Minh Khang VU
 */
public class InvalidDeploymentSetupException extends TechnicalException {

    public InvalidDeploymentSetupException(String message) {
        super(message);
    }

    public InvalidDeploymentSetupException(String message, Throwable cause) {
        super(message, cause);
    }
}
