package alien4cloud.exception;

/**
 * Exception to be thrown when trying create a location an parse an CSAR form plugin before this dependencies.
 */
public class MissingCSARDependenciesException extends TechnicalException {
    private static final long serialVersionUID = -6151150122897145637L;

    /**
     * Create a new {@link MissingCSARDependenciesException} with the cause.
     *
     * @param message Message.
     */
    public MissingCSARDependenciesException(String message) {
        super(message);
    }
}