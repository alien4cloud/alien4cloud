package alien4cloud.component.repository.exception;

public class RepositoryIOException extends RepositoryTechnicalException {

    private static final long serialVersionUID = -6341744275998225477L;

    public RepositoryIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositoryIOException(String message) {
        super(message);
    }

}
