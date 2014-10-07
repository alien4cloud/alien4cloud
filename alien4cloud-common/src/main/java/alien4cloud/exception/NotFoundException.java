package alien4cloud.exception;

public class NotFoundException extends TechnicalException {

    private static final long serialVersionUID = -5838741067731786413L;

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(String message) {
        super(message);
    }

}
