package alien4cloud.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * Exception to be thrown in case there is an issue with a git repository.
 */
@Getter
@Setter
public class GitConflictException extends GitException {
    private static final long serialVersionUID = -5917605742879793242L;

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
        super(String.format("Created a new branch=%s. Please merge it manually with the current branch=%s and push it into the remote=%s", conflictBranchName,
                branch, remoteName));
    }
}