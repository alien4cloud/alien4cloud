package alien4cloud.utils.version;

import alien4cloud.exception.TechnicalException;

public class InvalidVersionException extends TechnicalException {

    private static final long serialVersionUID = -5192834855057177252L;

    public InvalidVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidVersionException(String message) {
        super(message);
    }
}
