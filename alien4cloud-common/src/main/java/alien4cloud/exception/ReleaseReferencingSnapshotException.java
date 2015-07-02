package alien4cloud.exception;

/**
 * Thrown when attempting to create a release version of an application of a template while some dependencies remain in snapshot.
 */
public class ReleaseReferencingSnapshotException extends TechnicalException {
    private static final long serialVersionUID = 1L;

    public ReleaseReferencingSnapshotException(String message) {
        super(message);
    }
}
