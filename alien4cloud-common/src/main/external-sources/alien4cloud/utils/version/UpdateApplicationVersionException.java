package alien4cloud.utils.version;

import alien4cloud.exception.TechnicalException;

public class UpdateApplicationVersionException extends TechnicalException {

    private static final long serialVersionUID = -5192834855057177252L;

    public UpdateApplicationVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpdateApplicationVersionException(String message) {
        super(message);
    }
}
