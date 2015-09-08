package alien4cloud.paas.exception;

import alien4cloud.model.orchestrators.locations.ILocationMatcher;

/**
 * Exception to be thrown in case the {@link ILocationMatcher} fail to match a topology against registered locations.
 */
public class LocationMatchingException extends PaaSDeploymentException {
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param message
     */
    public LocationMatchingException(String message) {
        super(message);
    }

    public LocationMatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}