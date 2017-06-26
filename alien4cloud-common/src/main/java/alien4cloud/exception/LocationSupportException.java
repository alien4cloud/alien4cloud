package alien4cloud.exception;

/**
 * Generic exception to throw when location support violation
 */
public class LocationSupportException extends TechnicalException {
    private static final long serialVersionUID = -6151150122897145634L;

    public LocationSupportException(String message) {
        super(message);
    }
}
