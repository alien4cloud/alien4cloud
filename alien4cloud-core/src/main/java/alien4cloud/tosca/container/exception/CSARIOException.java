package alien4cloud.tosca.container.exception;

/**
 * Exception thrown for errors related to IO failure while accessing archive
 * 
 * @author mkv
 * 
 */
public class CSARIOException extends CSARTechnicalException {

    private static final long serialVersionUID = -4024742128655133535L;

    public CSARIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public CSARIOException(String message) {
        super(message);
    }

}
