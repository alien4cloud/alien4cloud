package alien4cloud.component.repository.exception;

import alien4cloud.exception.TechnicalException;

/**
 * All errors happened while trying to access to repository service
 * 
 * @author mkv
 * 
 */
public class RepositoryTechnicalException extends TechnicalException {

    private static final long serialVersionUID = 2216233217993040761L;

    public RepositoryTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositoryTechnicalException(String message) {
        super(message);
    }
}
