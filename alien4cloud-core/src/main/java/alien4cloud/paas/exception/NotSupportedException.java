package alien4cloud.paas.exception;

import alien4cloud.exception.TechnicalException;

/**
 * TechnicalException exception to be thrown when an operation or something is not yet supported in Alien4Cloud.
 */
public class NotSupportedException extends TechnicalException {
    private static final long serialVersionUID = 1L;

    public NotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSupportedException(String message) {
        super(message);
    }
}