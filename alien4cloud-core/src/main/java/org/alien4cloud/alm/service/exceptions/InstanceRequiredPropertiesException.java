package org.alien4cloud.alm.service.exceptions;

import java.util.Set;

import alien4cloud.exception.TechnicalException;
import lombok.Getter;

/**
 * Exception thrown when validation of required properties of a node instance fails.
 */
@Getter
public class InstanceRequiredPropertiesException extends TechnicalException {
    /** The properties that where required. */
    private final Set<String> properties;

    public InstanceRequiredPropertiesException(String message, Set<String> properties) {
        super(message);
        this.properties = properties;
    }
}
