package alien4cloud.exception;

/**
 * All functional exception which is related to user input must go here. It's a checked exception to force error handling and checking.
 * 
 * @author mkv
 * 
 */
public class FunctionalException extends Exception {

    private static final long serialVersionUID = 6712845685798792493L;

    public FunctionalException(String message, Throwable cause) {
        super(message, cause);
    }

    public FunctionalException(String message) {
        super(message);
    }
}
