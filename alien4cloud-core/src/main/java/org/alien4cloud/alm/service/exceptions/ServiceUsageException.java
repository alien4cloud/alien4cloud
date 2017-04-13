package org.alien4cloud.alm.service.exceptions;

import alien4cloud.exception.TechnicalException;
import alien4cloud.model.common.Usage;
import lombok.Getter;

/**
 * Exception to be thrown when an operation cannot be done because the service is used.
 */
@Getter
public class ServiceUsageException extends TechnicalException {
    private final Usage[] usages;

    public ServiceUsageException(String message, Usage[] usages) {
        super(message);
        this.usages = usages;
    }
}
