package alien4cloud.paas.wf.validation;

import java.util.List;

import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;

public interface Rule {

    List<AbstractWorkflowError> validate(TopologyContext topologyContext, Workflow workflow);

}
