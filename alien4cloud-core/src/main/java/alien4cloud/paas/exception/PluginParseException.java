package alien4cloud.paas.exception;

/**
 * Exception thrown in case a plugin parsing is incorrect.
 */
public class PluginParseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * New instance of the exception.
     *
     * @param message A message that details the exception reason.
     */
    public PluginParseException(String message) {
        super(message);
    }

    /**
     * New instance of the exception.
     *
     * @param message A message that details the exception reason.
     * @param cause The cause if any.
     */
    public PluginParseException(String message, Exception cause) {
        super(message, cause);
    }
}