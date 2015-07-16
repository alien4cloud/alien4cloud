package alien4cloud.exception;


/**
 * This exception is thrown when attempting to add a node in a topology that references the same topology (cyclic composition).
 */
public class CyclicReferenceException extends TechnicalException {
    private static final long serialVersionUID = 1L;

    public CyclicReferenceException(String message) {
        super(message);
    }
}
