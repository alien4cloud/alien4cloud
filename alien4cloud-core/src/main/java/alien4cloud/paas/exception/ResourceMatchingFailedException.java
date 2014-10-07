package alien4cloud.paas.exception;

import alien4cloud.paas.IPaaSProvider;

/**
 * Exception to be thrown in case the {@link IPaaSProvider} fail to find a resource for one of the elments in the topology.
 */
public class ResourceMatchingFailedException extends PaaSDeploymentException {
    private static final long serialVersionUID = 1L;

    /**
     * 
     * @param message
     */
    public ResourceMatchingFailedException(String message) {
        super(message);
    }

    public ResourceMatchingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}