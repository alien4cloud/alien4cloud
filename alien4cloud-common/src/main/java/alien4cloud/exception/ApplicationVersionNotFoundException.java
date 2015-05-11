package alien4cloud.exception;


public class ApplicationVersionNotFoundException extends TechnicalException {

    public ApplicationVersionNotFoundException(String message) {
        super(message);
    }

    public ApplicationVersionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
