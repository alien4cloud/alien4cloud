package org.alien4cloud.alm.service.exceptions;

import alien4cloud.exception.TechnicalException;
import alien4cloud.paas.model.DeploymentStatus;
import lombok.Getter;

/**
 * Exception thrown when trying to create a service out of a current invalid state (deploying or undeploying).
 */
@Getter
public class InvalidDeploymentStatusException extends TechnicalException {
    private DeploymentStatus deploymentStatus;

    public InvalidDeploymentStatusException(String message, DeploymentStatus deploymentStatus) {
        super(message);
        this.deploymentStatus = deploymentStatus;
    }
}
