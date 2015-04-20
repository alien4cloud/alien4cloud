package alien4cloud.paas.exception;


public class MaintenanceModeException extends PaaSTechnicalException {

    private static final long serialVersionUID = 1L;

    public MaintenanceModeException(String message, Throwable cause) {
        super(message, cause);

    }

    public MaintenanceModeException(String message) {
        super(message);
    }

}
