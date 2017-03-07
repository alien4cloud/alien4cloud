package alien4cloud.service.exceptions;

import java.util.Arrays;

import alien4cloud.exception.TechnicalException;
import alien4cloud.model.common.Usage;
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

    public Usage[] convert() {
        return usages == null ? null
                : Arrays.stream(usages).map(deployment -> new Usage(deployment.getSourceName(), "Deployment", deployment.getId(), null)).toArray(Usage[]::new);
    }
}
