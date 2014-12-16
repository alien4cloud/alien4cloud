package alien4cloud.paas.exception;

/**
 * This exception occurs when an issue in the topology definition prevents it from being deployed.
 */
public class InvalidTopologyException extends PaaSTechnicalException {

    /**
     * Creates an error that details the error.
     * 
     * @param message The message that explains the error.
     */
    public InvalidTopologyException(String message) {
        super(message);
    }
}