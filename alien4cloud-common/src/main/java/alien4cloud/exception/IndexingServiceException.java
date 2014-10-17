package alien4cloud.exception;


/**
 * All errors happened while trying to access to index service
 * 
 * @author mkv
 */
public class IndexingServiceException extends TechnicalException {

    private static final long serialVersionUID = 8644422735660389058L;

    public IndexingServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexingServiceException(String message) {
        super(message);
    }
}
