package alien4cloud.topology.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * Represent one task to do to have a deployable topology
 * 
 * @author 'Igor Ngouagna'
 * 
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractTask {
    // task code
    private TaskCode code;
    private String source;

    public AbstractTask(TaskCode code) {
        this.code = code;
    }
}