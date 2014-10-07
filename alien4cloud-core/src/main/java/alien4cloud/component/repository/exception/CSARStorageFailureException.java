package alien4cloud.component.repository.exception;

/**
 * Exception thrown when error occur while trying to store a CSAR
 * 
 * @author 'Igor Ngouagna'
 * 
 */
public class CSARStorageFailureException extends RepositoryTechnicalException {

    private static final long serialVersionUID = 6899360066701812903L;

    public CSARStorageFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public CSARStorageFailureException(String message) {
        super(message);
    }

}
