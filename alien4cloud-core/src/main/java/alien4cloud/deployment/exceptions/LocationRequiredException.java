package alien4cloud.deployment.exceptions;

import alien4cloud.exception.TechnicalException;

/**
 * Exception to be thrown if users tries to deploy a topology that doesn't specifies a valid location.
 */
public class LocationRequiredException extends TechnicalException {
    public LocationRequiredException(String message) {
        super(message);
    }
}
