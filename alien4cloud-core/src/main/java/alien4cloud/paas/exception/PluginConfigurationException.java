package alien4cloud.paas.exception;

import alien4cloud.exception.FunctionalException;

/**
 * Exception thrown in case a plugin configuration is incorrect.
 */
public class PluginConfigurationException extends FunctionalException {
    private static final long serialVersionUID = 1L;

    /**
     * New instance of the exception.
     * 
     * @param message A message that details the exception reason.
     */
    public PluginConfigurationException(String message) {
        super(message);
    }

    /**
     * New instance of the exception.
     * 
     * @param message A message that details the exception reason.
     * @param cause The cause if any.
     */
    public PluginConfigurationException(String message, Exception cause) {
        super(message, cause);
    }
}