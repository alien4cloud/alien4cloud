package alien4cloud.deployment.exceptions;

import alien4cloud.exception.TechnicalException;

/**
 * Thrown when the conditions are not met to update a deployment on an orchestrator
 * 
 */
public class ImpossibleDeploymentUpdateException extends TechnicalException {

    public ImpossibleDeploymentUpdateException(String message) {
        super(message);
    }

    public ImpossibleDeploymentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
