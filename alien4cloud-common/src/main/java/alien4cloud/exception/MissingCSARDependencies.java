package alien4cloud.exception;

/**
 * Exception to be thrown when trying create a location an parse an CSAR form plugin before this dependencies.
 */
public class MissingCSARDependencies extends TechnicalException {
    private static final long serialVersionUID = -6151150122897145637L;

    /**
     * Create a new {@link MissingCSARDependencies} with the cause.
     *
     * @param message Message.
     */
    public MissingCSARDependencies(String message) {
        super(message);
    }
}