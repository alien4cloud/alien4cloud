package alien4cloud.component.repository.exception;

/**
 * Exception thrown when trying to store an existing version of a CSAR
 * 
 * @author 'Igor Ngouagna'
 * 
 */
public class CSARVersionAlreadyExistsException extends RepositoryFunctionalException {

    private static final long serialVersionUID = -7825281720911419035L;

    public CSARVersionAlreadyExistsException(String message) {
        super(message);
    }

}
