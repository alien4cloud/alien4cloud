package alien4cloud.exception;

/**
 * Exception thrown when error occur while trying to get the indexed model of a Type which is not supposed to be indexed
 * 
 * @author 'Igor Ngouagna'
 */
public class IndexingNotSupportedException extends IndexingServiceException {

    private static final long serialVersionUID = -4992840514086627583L;

    /**
     * See {@link Exception#Exception(String)}
     * 
     * @param message See {@link Exception#Exception(String)}
     */
    public IndexingNotSupportedException(String message) {
        super(message);
    }

    public IndexingNotSupportedException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructs a new exception for a specific non indexed class.
     * 
     * @param clazz
     */
    public IndexingNotSupportedException(Class<?> clazz) {
        this("Indexing of object of type " + clazz.getName() + " not supported!");
    }
}
