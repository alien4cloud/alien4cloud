package alien4cloud.paas.exception;

import alien4cloud.exception.TechnicalException;

/**
 * TechnicalException exception to be thrown when we
 */
public class EmptyMetaPropertyException extends TechnicalException {
    private static final long serialVersionUID = 1L;

    public EmptyMetaPropertyException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyMetaPropertyException(String message) {
        super(message);
    }
}