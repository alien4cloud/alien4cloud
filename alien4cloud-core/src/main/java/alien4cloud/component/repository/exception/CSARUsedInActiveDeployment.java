package alien4cloud.component.repository.exception;

/**
 * Exception thrown when trying to override a csar that is used in an active deployment
 * 
 * @author 'Igor Ngouagna'
 * 
 */
public class CSARUsedInActiveDeployment extends RepositoryFunctionalException {

    private static final long serialVersionUID = -7825281720911419035L;

    public CSARUsedInActiveDeployment(String message) {
        super(message);
    }

}
