package alien4cloud.component.repository.exception;

/**
 * Exception thrown when a CSAR version is not found.
 * 
 * @author 'Igor Ngouagna'
 * 
 */
public class CSARVersionNotFoundException extends RepositoryFunctionalException {

    private static final long serialVersionUID = 2749533712404712499L;

    public CSARVersionNotFoundException(String message) {
        super(message);
    }

}
