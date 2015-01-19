package alien4cloud.utils.version;

import alien4cloud.exception.TechnicalException;

public class ApplicationVersionException extends TechnicalException {

    private static final long serialVersionUID = -5192834855057177252L;

    public ApplicationVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationVersionException(String message) {
        super(message);
    }
}
