package alien4cloud.component.repository.exception;

/**
 * Exception thrown when error occur while trying to create the CSAR Repository
 * 
 * @author 'Igor Ngouagna'
 * 
 */
public class CSARDirectoryCreationFailureException extends RepositoryTechnicalException {

    private static final long serialVersionUID = -2069639204666432256L;

    public CSARDirectoryCreationFailureException(String message, Throwable cause) {
        super(message, cause);
    }

}
