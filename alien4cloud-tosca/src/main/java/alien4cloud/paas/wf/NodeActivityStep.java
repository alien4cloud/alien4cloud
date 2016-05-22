package alien4cloud.paas.wf;

import lombok.Getter;
import lombok.Setter;

/**
 * A wf step that belongs to a node and executes a task.
 */
@Getter
@Setter
public class NodeActivityStep extends AbstractStep {

    private AbstractActivity activity;

    private String nodeId;

    private String hostId;
    
    @Override
    public String getStepAsString() {
        return activity.getRepresentation();
    }

}