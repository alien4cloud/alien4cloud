package alien4cloud.paas.exception;

import alien4cloud.paas.IPaaSProvider;

/**
 * Exception to be thrown in case the {@link IPaaSProvider} fail to find a mandatory property for one of the elments in the topology.
 */
public class MissingPropertyException extends PaaSDeploymentException {
    private static final long serialVersionUID = 1L;

    /**
     * 
     * @param message
     */
    public MissingPropertyException(String message) {
        super(message);
    }

    public MissingPropertyException(String message, Throwable cause) {
        super(message, cause);
    }
}