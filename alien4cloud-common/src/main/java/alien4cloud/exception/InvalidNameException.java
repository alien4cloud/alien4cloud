package alien4cloud.exception;

import lombok.Getter;

/**
 * Generic exception to throw when the name is invalid.
 */
public class InvalidNameException extends TechnicalException {
    private static final long serialVersionUID = -6151150122897145634L;
    @Getter
    private String nameKey;
    private String nameValue;

    /**
     * Create a new {@link InvalidNameException} with the cause.
     *
     * @param nameKey Key that contains the invalid name (nodeName, relationshipName etc.).
     * @param nameValue Value that is an invalid name.
     * @param message Message to explain the wrong naming (expected pattern for example).
     */
    public InvalidNameException(String nameKey, String nameValue, String message) {
        super(message);
        this.nameKey = nameKey;
        this.nameValue = nameValue;
    }
}
