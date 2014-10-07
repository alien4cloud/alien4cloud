package alien4cloud.tosca.container.exception;

import alien4cloud.exception.FunctionalException;

/**
 * All functional exception related to csar processing must go here
 * 
 * @author mkv
 * 
 */
public class CSARFunctionalException extends FunctionalException {

    private static final long serialVersionUID = -5273097067372057229L;

    public CSARFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }

    public CSARFunctionalException(String message) {
        super(message);
    }

}
