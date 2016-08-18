package alien4cloud.git;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Simplified git history entry
 */
@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
public class SimpleGitHistoryEntry {
    private String id;
    private String userName;
    private String userEmail;
    private String commitMessage;
    private Date date;
}
