package alien4cloud.exception;

public class NotFoundException extends TechnicalException {
    private static final long serialVersionUID = -5838741067731786413L;
    /** Type of the element not found. */
    private String type;
    /** Id of the element not found. */
    private String id;

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String type, String id, String message) {
        super(message);
        this.type = type;
    }
}
