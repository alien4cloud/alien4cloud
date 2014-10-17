package alien4cloud.paas.exception;

import alien4cloud.exception.TechnicalException;

public class PaaSTechnicalException extends TechnicalException {

    private static final long serialVersionUID = 1L;

    public PaaSTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaaSTechnicalException(String message) {
        super(message);
    }

}
