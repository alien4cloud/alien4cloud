package alien4cloud.tosca.container.exception;

import alien4cloud.exception.TechnicalException;

/**
 * Base class for all CSAR related exception
 * 
 * @author mkv
 * 
 */
public class CSARTechnicalException extends TechnicalException {

    private static final long serialVersionUID = -6409509450717513289L;

    public CSARTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    public CSARTechnicalException(String message) {
        super(message);
    }
}
