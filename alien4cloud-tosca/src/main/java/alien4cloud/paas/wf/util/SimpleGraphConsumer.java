package alien4cloud.paas.wf.util;

import java.util.Map;

import org.alien4cloud.tosca.model.workflow.WorkflowStep;

import lombok.Getter;

@Getter
public abstract class SimpleGraphConsumer implements GraphConsumer {

    private Map<String, WorkflowStep> allNodes;

    @Override
    public void onAllNodes(Map<String, WorkflowStep> allNodes) {
        this.allNodes = allNodes;
    }
}
