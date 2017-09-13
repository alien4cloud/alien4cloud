package alien4cloud.paas.wf.validation;

import java.util.ArrayList;
import java.util.List;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;

@Component
public class WorkflowValidator {

    private List<Rule> rules;

    public WorkflowValidator() {
        super();
        rules = new ArrayList<>();
        rules.add(new CycleDetection());
        rules.add(new StateSequenceValidation());
        rules.add(new NodeValidation());
    }

    public List<AbstractWorkflowError> validate(TopologyContext topologyContext, Workflow workflow) {
        List<AbstractWorkflowError> allErrors = new ArrayList<>();
        for (Rule rule : rules) {
            List<AbstractWorkflowError> errors = rule.validate(topologyContext, workflow);
            if (errors != null) {
                allErrors.addAll(errors);
            }
        }
        return allErrors;
    }

}
