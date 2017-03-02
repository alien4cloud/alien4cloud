package alien4cloud.service.exceptions;

import alien4cloud.exception.TechnicalException;
import alien4cloud.model.deployment.Deployment;
import lombok.Getter;

/**
 * Exception to be thrown when an operation cannot be done because the service is used.
 */
@Getter
public class ServiceUsageException extends TechnicalException {
    private final Deployment[] usages;

    public ServiceUsageException(String message, Deployment[] usages) {
        super(message);
        this.usages = usages;
    }
}
