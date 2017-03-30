package alien4cloud.paas.wf;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A wf step that belongs to a node and executes a task.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeActivityStep extends AbstractStep {

    private String nodeId;

    private String hostId;

    private AbstractActivity activity;

    @Override
    public String getStepAsString() {
        return activity.getRepresentation();
    }

}