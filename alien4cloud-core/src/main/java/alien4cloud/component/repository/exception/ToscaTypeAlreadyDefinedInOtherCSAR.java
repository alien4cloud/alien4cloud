package alien4cloud.component.repository.exception;

/**
 * Exception thrown when trying to override a node type that is defined in an other CSAR
 *
 */
public class ToscaTypeAlreadyDefinedInOtherCSAR extends RepositoryFunctionalException {

    private static final long serialVersionUID = -7825281720911419035L;

    public ToscaTypeAlreadyDefinedInOtherCSAR(String message) {
        super(message);
    }

}
