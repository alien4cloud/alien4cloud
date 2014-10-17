package alien4cloud.it.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Exception which is triggered by IT test it-self and not alien
 *
 * @author mkv
 *
 */
public class ITException extends TechnicalException {
    private static final long serialVersionUID = 1970578297756505647L;

    public ITException(String message, Throwable cause) {
        super(message, cause);
    }

    public ITException(String message) {
        super(message);
    }

}
