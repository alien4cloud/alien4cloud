package alien4cloud.exception;

/**
 * Exception to be thrown in case of an error that prevents alien to startup.
 */
public class InitializationException extends TechnicalException {
    public InitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
