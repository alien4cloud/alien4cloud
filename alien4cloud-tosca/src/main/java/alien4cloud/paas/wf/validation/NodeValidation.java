package alien4cloud.paas.wf.validation;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Lists;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.NodeActivityStep;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.exception.WorkflowException;

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
        for (AbstractStep step : workflow.getSteps().values()) {
            if (step instanceof NodeActivityStep) {
                String nodeId = ((NodeActivityStep) step).getNodeId();
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
