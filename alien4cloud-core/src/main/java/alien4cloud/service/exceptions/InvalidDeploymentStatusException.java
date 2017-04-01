package alien4cloud.service.exceptions;

import alien4cloud.exception.TechnicalException;
import alien4cloud.paas.model.DeploymentStatus;

/**
 * Exception thrown when trying to create a service out of a current invalid state (deploying or undeploying).
 */
public class InvalidDeploymentStatusException extends TechnicalException {
    private DeploymentStatus deploymentStatus;

    public InvalidDeploymentStatusException(String message, DeploymentStatus deploymentStatus) {
        super(message);
        this.deploymentStatus = deploymentStatus;
    }
}
