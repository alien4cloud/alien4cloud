package alien4cloud.paas.wf.util;

import org.alien4cloud.tosca.model.workflow.Workflow;

public interface GraphTraversal {

    Workflow getWorkflow();

    void browse();
}
