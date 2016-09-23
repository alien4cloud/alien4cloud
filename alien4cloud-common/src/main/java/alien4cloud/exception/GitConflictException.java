package alien4cloud.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * Exception to be thrown in case there is an issue with a git repository.
 */
@Getter
@Setter
public class GitConflictException extends TechnicalException {
    private static final long serialVersionUID = -5917605742879793240L;

    private String remoteName;
    private String branch;
    private String conflictBranchName;

    public GitConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitConflictException(String branch) {
        super(String.format("Conflict detected on current branch=%s", branch));
    }

    public GitConflictException(String remoteName, String branch, String conflictBranchName) {
        super(String.format("Created a new branch=%s while waiting for a merge with the current branch=%s on remote=%s",conflictBranchName, branch, remoteName));
    }


}