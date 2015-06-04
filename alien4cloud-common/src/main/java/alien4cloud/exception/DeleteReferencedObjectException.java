package alien4cloud.exception;

import java.util.List;

import lombok.Getter;

/**
 * This exception should be thrown when attempting to delete an object in Alien which is still referenced (used) by other object
 *
 * @author Minh Khang VU
 */
public class DeleteReferencedObjectException extends TechnicalException {
	
    private static final long serialVersionUID = 1L;
    @Getter
    private List<Object> dependencies;

    public DeleteReferencedObjectException(String message) {
        super(message);
    }

	public DeleteReferencedObjectException(String message, List<Object> dependencies) {
		super(message);
		this.dependencies = dependencies;
	}
}
