package alien4cloud.paas.wf.validation;

import java.util.List;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.elasticsearch.common.collect.Lists;

import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.exception.WorkflowException;
import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
public class NodeValidation implements Rule {

    @Override
    public List<AbstractWorkflowError> validate(TopologyContext topologyContext, Workflow workflow) throws WorkflowException {
        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            return null;
        }
        List<AbstractWorkflowError> errors = Lists.newArrayList();
        for (WorkflowStep step : workflow.getSteps().values()) {
            if (step instanceof NodeWorkflowStep) {
                String nodeId = step.getTarget();
                NodeTemplate nodeTemplate = null;
                if (topologyContext.getTopology().getNodeTemplates() != null) {
                    nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeId);
                }
                if (nodeTemplate == null) {
                    errors.add(new UnknownNodeError(step.getName(), nodeId));
                } else {
                    // TODO: here we should check interface & operation
                }
            }
        }
        return errors;
    }
}
