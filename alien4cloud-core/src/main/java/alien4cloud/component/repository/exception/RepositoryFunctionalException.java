package alien4cloud.component.repository.exception;

import alien4cloud.exception.FunctionalException;

/**
 * All functional exceptions related to repostory service should go here
 * 
 * @author mkv
 * 
 */
public class RepositoryFunctionalException extends FunctionalException {

    private static final long serialVersionUID = -4010898434310082550L;

    public RepositoryFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositoryFunctionalException(String message) {
        super(message);
    }

}
