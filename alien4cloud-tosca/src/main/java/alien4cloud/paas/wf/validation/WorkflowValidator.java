package alien4cloud.paas.wf.validation;

import java.util.ArrayList;
import java.util.List;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;

@Component
public class WorkflowValidator {
    public static ThreadLocal<Boolean> disableValidationThreadLocal = new ThreadLocal<>();
    private List<Rule> rules;

    public WorkflowValidator() {
        super();
        rules = new ArrayList<>();
        rules.add(new CycleDetection());
        rules.add(new SemanticValidation());
    }

    public int validate(TopologyContext topologyContext, Workflow workflow) {
        if (disableValidationThreadLocal.get() != null && disableValidationThreadLocal.get().booleanValue()) {
            // we just skip workflow validation.
            return 0;
        }
        workflow.clearErrors();
        int errorCount = 0;
        for (Rule rule : rules) {
            List<AbstractWorkflowError> errors = rule.validate(topologyContext, workflow);
            if (errors != null) {
                workflow.addErrors(errors);
                errorCount += errors.size();
            }
        }
        return errorCount;
    }

}
