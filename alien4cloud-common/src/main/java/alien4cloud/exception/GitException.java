package alien4cloud.exception;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;

/**
 * Exception to be thrown in case there is an issue with a git repository.
 */
public class GitException extends TechnicalException {
    private static final long serialVersionUID = -5917605742879793240L;

    public GitException(String message) {
        super(message);
    }

    public GitException(String message, Throwable cause) {
        super(message, cause);
    }

    public static GitException buildErrorOnReference(GitAPIException e, String reference) {
        if (e instanceof RefAlreadyExistsException) {
            return new GitException("A reference <" + reference + "> already exist.", e.getCause());
        } else if (e instanceof InvalidRefNameException) {
            return new GitException("The reference <" + reference + "> is invalid.", e.getCause());
        } else if (e instanceof RefNotFoundException) {
            return new GitException("The reference <" + reference + "> is not found.", e.getCause());
        } else {
            return new GitException(e.getMessage());
        }
    }
}