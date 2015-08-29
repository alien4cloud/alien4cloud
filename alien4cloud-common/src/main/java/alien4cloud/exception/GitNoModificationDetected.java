package alien4cloud.exception;

public class GitNoModificationDetected extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -6796309034989207112L;

    public GitNoModificationDetected(String message, Throwable cause) {
        super(message, cause);
    }

    public GitNoModificationDetected(String message) {
        super(message);
    }
}
