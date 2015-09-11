package alien4cloud.paas.wf.validation;

import java.util.List;

import alien4cloud.paas.wf.Workflow;

public interface Rule {

    List<AbstractWorkflowError> validate(Workflow workflow);

}
