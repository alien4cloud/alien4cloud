package alien4cloud.paas.wf;

import lombok.Getter;
import lombok.Setter;

/**
 * An activity that means: this node's lifecycle will be managed by orchestrator.
 * <p>
 * This is used when we don't want the lifecycle for a given (abstract) node to be concerned by customizations.
 */
@Getter
@Setter
public class DelegateWorkflowActivity extends AbstractActivity {

    private String workflowName;

    @Override
    public String toString() {
        return getNodeId() + ".call[" + workflowName + "]";
    }

    @Override
    public String getRepresentation() {
        return getNodeId() + "_" + workflowName;
    }

}